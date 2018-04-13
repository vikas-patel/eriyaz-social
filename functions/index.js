var functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

const promisePool = require('es6-promise-pool');
const PromisePool = promisePool.PromisePool;

const actionTypeNewRating = "new_rating"
const actionTypeNewComment = "new_comment"
const actionTypeNewPost = "new_post"
const notificationTitle = "RateMySinging"

const postsTopic = "postsTopic"
// Maximum concurrent database connection.
const MAX_CONCURRENT = 3;

exports.pushNotificationRatings = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onCreate(event => {

    console.log('New rating was added');

    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;

    // Get rated post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {

        if (ratingAuthorId == post.val().authorId) {
            return console.log('User rated own post');
        }

        // Get the list of device notification tokens.
        const getDeviceTokensTask = admin.database().ref(`/profiles/${post.val().authorId}/notificationTokens`).once('value');
        console.log('getDeviceTokensTask path: ', `/profiles/${post.val().authorId}/notificationTokens`)

        // Get rating author.
        const getRatingAuthorProfileTask = admin.database().ref(`/profiles/${ratingAuthorId}`).once('value');

        Promise.all([getDeviceTokensTask, getRatingAuthorProfileTask]).then(results => {
            const tokensSnapshot = results[0];
            const ratingAuthorProfile = results[1].val();

            // Check if there are any device tokens.
            if (!tokensSnapshot.hasChildren()) {
                return console.log('There are no notification tokens to send to.');
            }

            console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');
            console.log('Fetched rating Author profile', ratingAuthorProfile);

            // Create a notification
            const payload = {
                data : {
                    actionType: actionTypeNewRating,
                    title: notificationTitle,
                    body: `${ratingAuthorProfile.username} rated your post`,
                    icon: post.val().imagePath,
                    postId: postId,

                },
            };

            // Listing all tokens.
            const tokens = Object.keys(tokensSnapshot.val());
            console.log('tokens:', tokens[0]);

            // Send notifications to all tokens.
            return admin.messaging().sendToDevice(tokens, payload).then(response => {
                        // For each message check if there was an error.
                        const tokensToRemove = [];
                response.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        console.error('Failure sending notification to', tokens[index], error);
                        // Cleanup the tokens who are not registered anymore.
                        if (error.code === 'messaging/invalid-registration-token' ||
                            error.code === 'messaging/registration-token-not-registered') {
                            tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
                        }
                    }
                });
                return Promise.all(tokensToRemove);
            });
        });
    })
});

exports.pushNotificationComments = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {

    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const comment = event.data.val();

    console.log('New comment was added, id: ', postId);

    // Get the commented post .
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {



        // Get the list of device notification tokens.
        const getDeviceTokensTask = admin.database().ref(`/profiles/${post.val().authorId}/notificationTokens`).once('value');
        console.log('getDeviceTokensTask path: ', `/profiles/${post.val().authorId}/notificationTokens`)

        // Get post author.
        const getCommentAuthorProfileTask = admin.database().ref(`/profiles/${comment.authorId}`).once('value');
        console.log('getCommentAuthorProfileTask path: ', `/profiles/${comment.authorId}`)

        Promise.all([getDeviceTokensTask, getCommentAuthorProfileTask]).then(results => {
            const tokensSnapshot = results[0];
            const commentAuthorProfile = results[1].val();

            if (commentAuthorProfile.id == post.val().authorId) {
                return console.log('User commented own post');
            }

            // Check if there are any device tokens.
            if (!tokensSnapshot.hasChildren()) {
                return console.log('There are no notification tokens to send to.');
            }

            console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');

            // Create a notification
            const payload = {
                data : {
                    actionType: actionTypeNewComment,
                    title: notificationTitle,
                    body: `${commentAuthorProfile.username} commented your post`,
                    icon: post.val().imagePath,
                    postId: postId,
                },
            };

            // Listing all tokens.
            const tokens = Object.keys(tokensSnapshot.val());
            console.log('tokens:', tokens[0]);

            // Send notifications to all tokens.
            return admin.messaging().sendToDevice(tokens, payload).then(response => {
                        // For each message check if there was an error.
                        const tokensToRemove = [];
                response.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        console.error('Failure sending notification to', tokens[index], error);
                        // Cleanup the tokens who are not registered anymore.
                        if (error.code === 'messaging/invalid-registration-token' ||
                            error.code === 'messaging/registration-token-not-registered') {
                            tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
                        }
                    }
                });
                return Promise.all(tokensToRemove);
            });
        });
    })
});

exports.pushNotificationPostNew = functions.database.ref('/posts/{postId}').onCreate(event => {
    const postId = event.params.postId;
    console.log('New post was created');

    // Get post authorID.
    const getAuthorIdTask = admin.database().ref(`/posts/${postId}/authorId`).once('value');

     return getAuthorIdTask.then(authorId => {

        console.log('post author id', authorId.val());

          // Create a notification
        const payload = {
            data : {
                actionType: actionTypeNewPost,
                postId: postId,
                authorId: authorId.val(),
            },
        };

        // Send a message to devices subscribed to the provided topic.
        return admin.messaging().sendToTopic(postsTopic, payload)
                 .then(function(response) {
                   // See the MessagingTopicResponse reference documentation for the
                   // contents of response.
                   console.log("Successfully sent info about new post :", response);
                 })
                 .catch(function(error) {
                   console.log("Error sending info about new post:", error);
                 });
         });

});

// Keeps track of the length of the 'likes' child list in a separate property.
exports.updatePostCounters = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onWrite(event => {
    const postRatingRef = event.data.ref.parent.parent;
    const postId = event.params.postId;
    console.log('Rating changed on post ', postId);
	
    return postRatingRef.once('value').then(snapshot => {
        let ratingTotal = 0;
        let ratingNum = snapshot.numChildren();
        snapshot.forEach(function(authorSnap) {
	      authorSnap.forEach(function(ratingSnap) {
		     ratingTotal = ratingTotal + ratingSnap.val().rating;
	      });
        });
        console.log("ratingTotal ", ratingTotal);
        // Get the rated post
        const postRef = admin.database().ref(`/posts/${postId}`);
        return postRef.transaction(current => {
            if (current == null) {
                console.log("ignore: null object returned from cache, expect another event with fresh server value.");
                return false;
            }
            current.ratingsCount = ratingNum;
            if (ratingNum > 0) {
                current.averageRating = ratingTotal/ratingNum;
            } else {
                current.averageRating = 0;
            }
            return current;
        }).then(() => {
            console.log('Post counters updated.');
        });
   });
});

// exports.commentsPoints = functions.database.ref('/post-comments/{postId}/{commentId}').onWrite(event => {

//     if (event.data.exists() && event.data.previous.exists()) {
//         return console.log("no points for comment updates");
//     }
//     const commentId = event.params.commentId;
//     const postId = event.params.postId;
//     const comment = event.data.exists() ? event.data.val() : event.data.previous.val();
//     const commentAuthorId = comment.authorId;
//     const comment_points = 2;

//     console.log('New comment was added, post id: ', postId);

//     // Get the commented post .
//     const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

//     return getPostTask.then(post => {

//         if (commentAuthorId == post.val().authorId) {
//             return console.log('User commented on own post');
//         }

//         // Get user points ref
//         const userPointsRef = admin.database().ref(`/user-points/${commentAuthorId}`);
//         var newPointRef = userPointsRef.push();
//         newPointRef.set({
//             'action': event.data.exists() ? "add":"remove",
//             'type': 'comment',
//             'value': event.data.exists() ? comment_points:-comment_points,
//             'creationDate': admin.database.ServerValue.TIMESTAMP
//         });

//         // Get rating author.
//         const authorProfilePointsRef = admin.database().ref(`/profiles/${commentAuthorId}/points`);
//         return authorProfilePointsRef.transaction(current => {
//             if (event.data.exists()) {
//               return (current || 0) + comment_points;
//             } else {
//               return (current || 0) - comment_points;
//             }
//         }).then(() => {
//             console.log('User comment points updated.');
//         });

//     })
// });

// Two different fuctions for post add and remove, because there were too many post update request
// and firebase has restriction on frequency of function calls.
exports.postAddedPoints = functions.database.ref('/posts/{postId}').onCreate(event => {
    const postId = event.params.postId;
    const post = event.data.val();
    const postAuthorId = post.authorId;
    var post_points = 3;
    console.log('Post created. ', postId);
    if (post.longRecording) post_points = 10;
    // Get user points ref
    const userPointsRef = admin.database().ref(`/user-points/${postAuthorId}`);
    var newPointRef = userPointsRef.push();
    newPointRef.set({
        'action': "add",
        'type': 'post',
        'value': -post_points,
        'creationDate': admin.database.ServerValue.TIMESTAMP
    });

    // Get rating author.
    const authorProfilePointsRef = admin.database().ref(`/profiles/${postAuthorId}/points`);
    return authorProfilePointsRef.transaction(current => {
          current = (current || 0) - post_points;
          if (current > 0) {
                return current;
          } else {
                return 0;
          }
    }).then(() => {
        console.log('User post added points updated.');
    });
});

// exports.postDeletePoints = functions.database.ref('/posts/{postId}').onDelete(event => {
//     const postId = event.params.postId;
//     const post = event.data.previous.val();
//     const postAuthorId = post.authorId;
//     console.log('Post removed. ', postId);
//     // Get user points ref
//     const userPointsRef = admin.database().ref(`/user-points/${postAuthorId}`);
//     var newPointRef = userPointsRef.push();
//     newPointRef.set({
//         'action': "remove",
//         'type': 'post',
//         'value': 5,
//         'creationDate': admin.database.ServerValue.TIMESTAMP
//     });

//     // Get rating author.
//     const authorProfilePointsRef = admin.database().ref(`/profiles/${postAuthorId}/points`);
//     return authorProfilePointsRef.transaction(current => {
//           return (current || 0) + 5;
//     }).then(() => {
//         console.log('User post points updated.');
//     });
// });

exports.ratingPoints = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onWrite(event => {
    if (event.data.exists() && event.data.previous.exists()) {
        return console.log("no points for rating updates");
    }
    console.log('Points for new/remove rating');

    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;

    // Get rated post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {

        if (ratingAuthorId == post.val().authorId) {
            return console.log('User rated own post');
        }

        // Get user points ref
        const userPointsRef = admin.database().ref(`/user-points/${ratingAuthorId}`);
        var newPointRef = userPointsRef.push();
        newPointRef.set({
            'action': event.data.exists() ? "add":"remove",
            'type': 'rating',
            'value': event.data.exists() ? 1:-1,
            'creationDate': admin.database.ServerValue.TIMESTAMP
        });

        // Get rating author.
        const authorProfilePointsRef = admin.database().ref(`/profiles/${ratingAuthorId}/points`);
        return authorProfilePointsRef.transaction(current => {
            if (event.data.exists()) {
              return (current || 0) + 1;
            } else {
              return (current || 0) - 1;
            }
        }).then(() => {
            console.log('User rating points updated.');
        });

    })
});

exports.appNotificationRatings = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onCreate(event => {
    console.log('App notification for new rating');

    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;

    // Get rated post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {
        var postAuthorId = post.val().authorId;
        if (ratingAuthorId == postAuthorId) {
            return console.log('User rated own post');
        }
        // Get rating author.
        const getRatingAuthorProfileTask = admin.database().ref(`/profiles/${ratingAuthorId}`).once('value');

        return getRatingAuthorProfileTask.then(profile => {
            // Get user notification ref
            const userNotificationsRef = admin.database().ref(`/user-notifications/${postAuthorId}`);
            var newNotificationRef = userNotificationsRef.push();
            var msg = profile.val().username + " rated your post '" + post.val().title + "'";
            newNotificationRef.set({
                'action': 'com.eriyaz.social.activities.PostDetailsActivity',
                'fromUserId' : ratingAuthorId,
                'message': msg,
                'extraKey' : 'PostDetailsActivity.POST_ID_EXTRA_KEY',
                'extraKeyValue' : postId,
                'createdDate': admin.database.ServerValue.TIMESTAMP
            });
        });

    })
});

exports.duplicateUserRating = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onWrite(event => {
    console.log('Duplicate user rating');
    const ratingAuthorId = event.params.authorId;
    const ratingId = event.params.ratingId;
    const postId = event.params.postId;
    const rating = event.data.val();
    if (rating != null) rating.postId = postId;
    const userRatingRef = admin.database().ref(`/user-ratings/${ratingAuthorId}/${ratingId}`);
    return userRatingRef.set(rating);
});

exports.appNotificationComments = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {
    console.log('App notification for new comment');

    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const comment = event.data.val();
    const commentAuthorId = comment.authorId;

    // Get rated post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {
        var postAuthorId = post.val().authorId;
        if (commentAuthorId == postAuthorId) {
            return console.log('User commented on own post');
        }
        // Get comment author.
        const getCommentAuthorProfileTask = admin.database().ref(`/profiles/${commentAuthorId}`).once('value');

        return getCommentAuthorProfileTask.then(profile => {
            // Get user notification ref
            const userNotificationsRef = admin.database().ref(`/user-notifications/${postAuthorId}`);
            var newNotificationRef = userNotificationsRef.push();
            var msg = profile.val().username + " commented on your post '" + post.val().title + "'";
            newNotificationRef.set({
                'action': 'com.eriyaz.social.activities.PostDetailsActivity',
                'fromUserId' : commentAuthorId,
                'message': msg,
                'extraKey' : 'PostDetailsActivity.POST_ID_EXTRA_KEY',
                'extraKeyValue' : postId,
                'createdDate': admin.database.ServerValue.TIMESTAMP
            });
        });
    })
});

exports.appNotificationCommentConversation = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {
    console.log('App notification for new comment in conversation');

    const postCommentRef = event.data.ref.parent;
    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const comment = event.data.val();
    const commentAuthorId = comment.authorId;

    // Get commented post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {
        const postVal = post.val();
        console.log("new comment on post '", postVal.title,"'.");
        var postAuthorId = postVal.authorId;
        // process all post comments
        return postCommentRef.once('value').then(snapshot => {
            const authorToNotify = [];
            snapshot.forEach(function(commentSnap) {
                // exclude post author & comment user
                const notifyAuthorId = commentSnap.val().authorId;
                if (notifyAuthorId  != postAuthorId && notifyAuthorId != commentAuthorId ) {
                    if (!authorToNotify.includes(notifyAuthorId)) {
                        authorToNotify.push(notifyAuthorId);
                    }
                }
            });
            if (authorToNotify.length == 0) return;
            // Get comment author.
            const getCommentAuthorProfileTask = admin.database().ref(`/profiles/${commentAuthorId}`).once('value');
            return getCommentAuthorProfileTask.then(profile => {
                const promisePool = new PromisePool(() => {
                  if (authorToNotify.length > 0) {
                    const authorId = authorToNotify.pop();
                    // Get user notification ref
                    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
                    var newNotificationRef = userNotificationsRef.push();
                    var msg = profile.val().username + " commented on the post '" + postVal.title + "' on which you also commented.";
                    return newNotificationRef.set({
                        'action': 'com.eriyaz.social.activities.PostDetailsActivity',
                        'fromUserId' : commentAuthorId,
                        'message': msg,
                        'extraKey' : 'PostDetailsActivity.POST_ID_EXTRA_KEY',
                        'extraKeyValue' : postId,
                        'createdDate': admin.database.ServerValue.TIMESTAMP
                    }).then(() => {
                        console.log('sent new comment notification on post you commented to ', authorId);
                    });
                  }
                  return null;
                }, MAX_CONCURRENT);
                const poolTask =  promisePool.start();
                return poolTask.then(() => {
                    return console.log('sent notification task completed.');
                });
            });
        });
    });
});

exports.incrementUserUnseenNotification = functions.database.ref('/user-notifications/{authorId}/{notificationId}').onCreate(event => {
    const authorId = event.params.authorId;
    const authorProfileUnseenRef = admin.database().ref(`/profiles/${authorId}/unseen`);
    return authorProfileUnseenRef.transaction(current => {
          return (current || 0) + 1;
    }).then(() => {
        console.log('User unseen count incremented.');
    });
});

exports.taskPopulatePastRatedData = functions.https.onRequest((req, res) => {
    // check if security key is same
    const keyParam = req.query.key;
    const key = "Test!234";
    if (key != keyParam) {
        console.log('The key ', key,' provided in the request does not match the key set in the environment.');
        res.status(403).send('Security key does not match. Make sure your "key" URL query parameter matches the ' +
          'cron.key environment variable.');
        return null;
    }
    return nextPostRating(res);
});

function nextPostRating(res, key) {
    if (key == null) {
        return admin.database().ref(`/post-ratings/`).orderByKey().limitToFirst(4).once('value').then(snapshot => {
            var lastPostId;
            snapshot.forEach(function(child) {
                lastPostId = child.key;
                console.log(lastPostId);
            });
            return nextPostRating(res, lastPostId);
        });
    } else {
        return admin.database().ref(`/post-ratings/`).orderByKey().startAt(key).limitToFirst(4).once('value').then(snapshot => {
            var lastPostId;
            snapshot.forEach(function(child) {
                lastPostId = child.key;
                if (key == lastPostId) return;
                processAuthorRatingNode(child);
                console.log(lastPostId);
            });
            if (snapshot.numChildren() == 4) {
                return nextPostRating(res, lastPostId);
            } else {
                console.log("exit function");
                return res.status(200).send('User notification task finished');
            }
        });
    }
}

function processAuthorRatingNode(postSnap) {
    postSnap.forEach(function(authorSnap) {
        var postId = postSnap.key;
        console.log('Rating updated for post ', postId);
        const ratingAuthorId = authorSnap.key;
        authorSnap.forEach(function(ratingSnap) {
            const ratingId = ratingSnap.key;
            var ratingRef = admin.database().ref(`/post-ratings/${postId}/${ratingAuthorId}/${ratingId}`);
            return ratingRef.transaction(current => {
                if (current == null) {
                    console.log('Rating object null for post ', postId);
                    return false;
                }
                current.viewedByPostAuthor = true;
                return current;
            });
        });
    });
}

exports.appUpdateNotification = functions.https.onRequest((req, res) => {
    // check if security key is same
    const keyParam = req.query.key;
    const key = "Test!234";
    if (key != keyParam) {
        console.log('The key ', key,' provided in the request does not match the key set in the environment.');
        res.status(403).send('Security key does not match. Make sure your "key" URL query parameter matches the ' +
          'cron.key environment variable.');
        return null;
    }
    // Get profiles
    const getProfilesTask = admin.database().ref(`/profiles/`).once('value');
    return getProfilesTask.then(snapshot => {
        const snapshotVal = snapshot.val();
        const profiles = Object.keys(snapshotVal).map(key => snapshotVal[key]);
        // const profiles = Object.values(profilesSnapshot.val());
        // const profiles = profilesSnapshot.val();
        console.log("About to update ", profiles.length, ' users.');
        const msg = "New version of the app is available now with important updates. Tap to update.";
        const promisePool = new PromisePool(() => {
          if (profiles.length > 0) {
            const profile = profiles.pop();
            // Get user notification ref
            const authorId = profile.id;
            const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
            var newNotificationRef = userNotificationsRef.push();
            // add notification
            return newNotificationRef.set({
                'action': 'com.eriyaz.social',
                //'fromUserId' : '',
                'message': msg,
                'openPlayStore': true,
                'createdDate': admin.database.ServerValue.TIMESTAMP
            });
          }
          return null;
        }, MAX_CONCURRENT);
        const poolTask =  promisePool.start();
        return poolTask.then(() => {
            return console.log('added notification to user');
        }).catch((error) => {
            console.error('User notification failed:', error);
        }).then(() => {
            console.log('User notification task finished');
            return res.status(200).send('User notification task finished');
        });
    });
});

// exports.appUninstall = functions.analytics.event('app_remove').onLog(event => {
//     const user = event.data.user;
//     const uid = user.userId;
//     console.log("app uninstall detected for uid ",uid);
//     // Get profile of user and set user details
//     if (uid) {
//         const profileUninstallRef = admin.database().ref(`/uninstall/track/${uid}`);
//         const newProfileUninstallRef = profileUninstallRef.push();
//         user.uninstallTime = event.data.logTime;
//         return newProfileUninstallRef.set(user).then(() => {
//             console.log('uninstall user profile updated with event details.');
//         });
//     } else {
//         const appInstanceId = user.appInfo.appInstanceId;
//         const uninstallUntrackRef = admin.database().ref(`/uninstall/untrack/${appInstanceId}`);
//         const newUninstallUntrackRef = uninstallUntrackRef.push();
//         user.uninstallTime = event.data.logTime;
//         return newUninstallUntrackRef.set(user).then(() => {
//             console.log('updated uninstall details of untracked user.');
//         });
//     }
// });

// exports.incrementUserMessageCount = functions.database.ref('/user-messages/{authorId}/{messageId}').onCreate(event => {
//     const authorId = event.params.authorId;
//     const authorProfileMessageCountRef = admin.database().ref(`/profiles/${authorId}/messageCount`);
//     return authorProfileMessageCountRef.transaction(current => {
//           return (current || 0) + 1;
//     }).then(() => {
//         console.log('User message count incremented.');
//     });
// });

// exports.decrementUserMessageCount = functions.database.ref('/user-messages/{authorId}/{messageId}').onDelete(event => {
//     const authorId = event.params.authorId;
//     const authorProfileMessageCountRef = admin.database().ref(`/profiles/${authorId}/messageCount`);
//     return authorProfileMessageCountRef.transaction(current => {
//           return (current || 1) - 1;
//     }).then(() => {
//         console.log('User message count decremented.');
//     });
// });

exports.appNotificationMessages = functions.database.ref('/user-messages/{userId}/{messageId}').onCreate(event => {
    console.log('App notification for new message');

    const messageId = event.params.messageId;
    const userId = event.params.userId;
    const message = event.data.val();
    const messageAuthorId = message.senderId;

    if (messageAuthorId == userId) {
        return console.log('User messaged on own wall');
    }
    // Get message author.
    const getMessageAuthorProfileTask = admin.database().ref(`/profiles/${messageAuthorId}`).once('value');

    return getMessageAuthorProfileTask.then(profile => {
        // Get user notification ref
        const userNotificationsRef = admin.database().ref(`/user-notifications/${userId}`);
        var newNotificationRef = userNotificationsRef.push();
        var msg = profile.val().username + " left a message on your profile page.";
        newNotificationRef.set({
            'action': 'com.eriyaz.social.activities.MessageActivity',
            'fromUserId' : messageAuthorId,
            'message': msg,
            'extraKey' : 'ProfileActivity.USER_ID_EXTRA_KEY',
            'extraKeyValue' : userId,
            'createdDate': admin.database.ServerValue.TIMESTAMP
        });
    });
});
