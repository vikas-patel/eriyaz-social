var functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
var https = require('https');

const promisePool = require('es6-promise-pool');
const PromisePool = promisePool.PromisePool;
var paytm_config = require('./paytm/paytm_config').paytm_config;
var paytm_checksum = require('./paytm/checksum');
const nodemailer = require('nodemailer');

const actionTypeNewRating = "new_rating";
const actionTypeOfficialFeedback = "official_feedback";
const actionTypeNewComment = "new_comment";
const actionTypeNewPost = "new_post";
const notificationTitle = "RateMySinging";

const postsTopic = "postsTopic"
// Maximum concurrent database connection.
const MAX_CONCURRENT = 3;
const REWARD_POINTS = 20;
const MAX_SUPPORTING_RATINGS = 4;
const MIN_SUPPORTING_RATINGS = 2;
const MIN_RATING_MINUTE = 2;
const MAX_RATING_MINUTE = 120;
const db = admin.database();
var supportingAuthorIds;
var masterAuthorIds;
// var recentPosts;
const cacheDays = 2*24*60*60*1000;
const WELCOME_ADMIN = functions.config().app.environment === 'dev' ? 'dsUhfoavcLUH4xsisgWW30N5v1u1' : 'eOaCAHXB8qfPKx1ZEKqSZXqrnXi2';

const gmailEmail = functions.config().gmail.email;
const gmailPassword = functions.config().gmail.password;
const mailTransport = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: gmailEmail,
    pass: gmailPassword,
  },
});

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
                    icon: ratingAuthorProfile.photoUrl ? ratingAuthorProfile.photoUrl : "",
                    postId: postId,
                    postTitle: post.val().title,
                    authorName: ratingAuthorProfile.username

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

function sendPushNotification(senderId, receiverId, postId, body) {
    // Get the list of device notification tokens.
    const getDeviceTokensTask = admin.database().ref(`/profiles/${receiverId}/notificationTokens`).once('value');
    console.log('getDeviceTokensTask path: ', `/profiles/${receiverId}/notificationTokens`)

    // Get rating author.
    const getRatingAuthorProfileTask = admin.database().ref(`/profiles/${senderId}`).once('value');

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
                actionType: actionTypeOfficialFeedback,
                title: notificationTitle,
                body: `${ratingAuthorProfile.username} ${body}`,
                icon: ratingAuthorProfile.photoUrl ? ratingAuthorProfile.photoUrl: "",
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
}

function sendChatPushNotification(senderId, receiverId, body, clickActivity, extraKeyValue) {
    // Get the list of device notification tokens.
    const getDeviceTokensTask = admin.database().ref(`/profiles/${receiverId}/notificationTokens`).once('value');
    console.log('getDeviceTokensTask path: ', `/profiles/${receiverId}/notificationTokens`)

    // Get rating author.
    const getReceiverProfileTask = admin.database().ref(`/profiles/${senderId}`).once('value');

    Promise.all([getDeviceTokensTask, getReceiverProfileTask]).then(results => {
        const tokensSnapshot = results[0];
        const senderProfile = results[1].val();

        // Check if there are any device tokens.
        if (!tokensSnapshot.hasChildren()) {
            return console.log('There are no notification tokens to send to.');
        }

        console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');
        console.log('Fetched rating Author profile', senderProfile);

        // Create a notification
        const payload = {
            notification : {
                title: notificationTitle,
                body: body,
                sound: "default",
                click_action: clickActivity
            },
            data : {
                'ProfileActivity.USER_ID_EXTRA_KEY' : extraKeyValue
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
}

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
                    postTitle: post.val().title,
                    body: `${commentAuthorProfile.username} commented your post`,
                    icon: commentAuthorProfile.photoUrl ? commentAuthorProfile.photoUrl : "",
                    authorName: commentAuthorProfile.username,
                    postId: postId
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

// exports.emailFeedback = functions.database.ref('/feedbacks/{feedbackId}').onCreate(event => {
//   const val = event.data.val()

//   const mailOptions = {
//     from: '"RateMySinging App" <eriyazonline@gmail.com>',
//     to: gmailEmail,
//   };

//   // Building Email message.
//   mailOptions.subject = 'RateMySinging Feedback';
//   mailOptions.text = val.text;

//   return mailTransport.sendMail(mailOptions)
//     .then(() => console.log(`email sent`))
//     .catch((error) => console.error('There was an error while sending the email:', error));
// });

function sendEmail(subject, body) {
    const mailOptions = {
        from: '"RateMySinging App" <eriyazonline@gmail.com>',
        to: gmailEmail,
      };

      // Building Email message.
      mailOptions.subject = subject;
      mailOptions.text = body;

      return mailTransport.sendMail(mailOptions)
        .then(() => console.log(`email sent`))
        .catch((error) => console.error('There was an error while sending the email:', error));
}

// Keeps track of the length of the 'likes' child list in a separate property.
exports.updatePostCounters = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}/normalizedRating').onWrite(event => {
    if (event.data.exists() && !event.data.previous.exists() && event.data.val() == 0) {
        console.log("ignore: normalizedRating hasn't set yet.");
        return 0;
    }
    const postRatingRef = event.data.ref.parent.parent.parent;
    const postId = event.params.postId;
    console.log('updating post counters ', postId);

    return postRatingRef.once('value').then(snapshot => {
        var ratingTotal = 0;
        var removedRatingsCount = 0;
        var ratingNum = snapshot.numChildren();
        snapshot.forEach(function(authorSnap) {
	      authorSnap.forEach(function(ratingSnap) {
             let ratingVal = ratingSnap.val().normalizedRating;
             if (!ratingVal) {
               ratingVal = ratingSnap.val().rating;
             }
             if(!ratingSnap.val().ratingRemoved){
		           ratingTotal = ratingTotal + ratingVal;
             }
            else{
              removedRatingsCount++;
            }
	      });
        });
        // Get the rated post
        const postRef = admin.database().ref(`/posts/${postId}`);
        return postRef.transaction(current => {
            if (current == null) {
                console.log("ignore: null object returned from cache, expect another event with fresh server value.");
                return null;
            }
            current.ratingsCount = ratingNum;
            if (ratingNum > 0 && ratingNum!=removedRatingsCount) {
                current.averageRating = ratingTotal/(ratingNum-removedRatingsCount);
            } else {
                current.averageRating = 0;
            }
            return current;
        }).then(() => {
            console.log('Post counters updated.');
        });
   });
});

exports.updatePostBoughtFeedbackStatus = functions.database.ref('/bought-feedbacks/{postId}').onWrite(event => {
    const postId = event.params.postId;
    const feedback = event.data.val();
    if (feedback.paymentStatus != "TXN_SUCCESS" && feedback.paymentStatus != "PENDING") {
        console.log("paymentStatus is ", feedback, ". So just exit.");
        return 0;
    }
    const isResolved = feedback.resolved;
    console.log('bought feedback status changed on post', postId, isResolved);

    const postRef = admin.database().ref(`/posts/${postId}`);
    return postRef.transaction(current => {
        if (current == null) {
            console.log("ignore: null object returned from cache, expect another event with fresh server value.");
            return null;
        }
        if (feedback.paymentStatus == "PENDING") {
            current.boughtFeedbackStatus = "PAYMENT_STATUS_PENDING";
        } else if (isResolved) {
            current.boughtFeedbackStatus = "GIVEN";
        } else {
            current.boughtFeedbackStatus = "ASKED";
        }
        return current;
    }).then(() => {
        console.log('Post bought feedback status updated.');
    });
});

exports.pushNotificationNewBoughtFeedback = functions.database.ref('/bought-feedbacks/{postId}').onCreate(event => {
    const postId = event.params.postId;
    const feedback = event.data.val();
    // Get Admin users
    const profileRef = admin.database().ref(`/profiles`);
    const profileQuery = profileRef.orderByChild('admin').equalTo(true).once('value');
    var promises = [];
    return profileQuery.then(adminsSnap => {
        adminsSnap.forEach(function(adminSnap) {
            // exclude parent author & message author
            const adminId = adminSnap.key;
            promises.push(sendPushNotification(feedback.authorId, adminId, postId, "asked for Official Feedback"));
        });
    });
    return Promise.all(promises).then(results => {
        console.log("sent push notifications to admins");
    });

});

exports.updatePostLastCommentDate = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {
    const postId = event.params.postId;
    console.log('updating post last comment date ', postId);
    const postRef = admin.database().ref(`/posts/${postId}`);
    return postRef.transaction(current => {
        if (current == null) {
            console.log("ignore: null object returned from cache, expect another event with fresh server value.");
            return null;
        }
        current.lastCommentDate = admin.database.ServerValue.TIMESTAMP;
        return current;
    }).then(() => {
        console.log('Post last comment date updated.');
    });
});

exports.rewardReputationPoints = functions.database.ref('/post-comments/{postId}/{commentId}/reputationPoints').onCreate(event => {
    const newPoints = event.data.val();
    if (!newPoints) return 0;
    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const commentRef = event.data.ref.parent;

    return commentRef.once('value').then(snapshot => {
        let comment = snapshot.val();
        const commentAuthorId = comment.authorId;
        return updateUserReputationPoints(commentAuthorId, newPoints, postId);
    });
});

exports.rewardReputationPointsUpdate = functions.database.ref('/post-comments/{postId}/{commentId}/reputationPoints').onUpdate(event => {
    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const commentRef = event.data.ref.parent;
    const newPoints = event.data.val();
    const previousPoints = event.data.previous.val();
    // don't show notification if points decremented
    if (newPoints <= previousPoints) return;
    return commentRef.once('value').then(snapshot => {
        let comment = snapshot.val();
        const commentAuthorId = comment.authorId;
        return updateUserReputationPoints(commentAuthorId, newPoints - previousPoints, postId, previousPoints != 0);
    });
});

exports.detailedFeedbackPoints = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {
    const commentId = event.params.commentId;
    const comment = event.data.val();
    const commentAuthorId = comment.authorId;
    const commentListRef = event.data.ref.parent;
    const comment_points = 1;
    if (!comment.detailedFeedback) return 0;
    console.log("reward extra points for detailed feedback");
    // Get all feedback with same parent feedback
    const getChildrenCommentTask = commentListRef.orderByChild('authorId').equalTo(commentAuthorId).once('value');
    return getChildrenCommentTask.then(snapshot => {
        let awarded = false;
        snapshot.forEach(function(commentSnap) {
            if (awarded) return;
            // exclude parent author & feedback author
            const commentItem = commentSnap.val();
            if (commentSnap.key != commentId && commentItem.detailedFeedback) {
                // already extra point rewarded.
                awarded = true;
                return;
            }
        });
        if (!awarded) {
            // Get user points ref
            const userPointsRef = admin.database().ref(`/user-points/${commentAuthorId}`);
            var newPointRef = userPointsRef.push();
            newPointRef.set({
                'action': "add",
                'type': 'detailed feedback',
                'value': comment_points,
                'creationDate': admin.database.ServerValue.TIMESTAMP
            });
            return addPoints(commentAuthorId, comment_points);
        }
    });
});

exports.ratingDetailedTextPoints = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}/detailedText').onCreate(event => {
    console.log("rating detailedText set");
    const ratingId = event.params.ratingId;
    const detailedText = event.data.val();
    const authorId = event.params.authorId;
    const commentListRef = event.data.ref.parent;
    const comment_points = 1;
    if (!detailedText) {
        console.log("empty detailed text")
        return;
    }
    const ratingRef = event.data.ref.parent;
    return ratingRef.once('value').then(snapshot => {
        var rating = snapshot.val();
        if (rating.rating <= 5 || rating.rating > 15) {
            console.log("no points for not ok and amazing rating");
            return;
        }
        console.log("reward extra points for detailed rating text");
        // Get user points ref
        const userPointsRef = admin.database().ref(`/user-points/${authorId}`);
        var newPointRef = userPointsRef.push();
        newPointRef.set({
            'action': "add",
            'type': 'detailed feedback',
            'value': comment_points,
            'creationDate': admin.database.ServerValue.TIMESTAMP
        });
        return addPoints(authorId, comment_points);
    });
});

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

function addPoints(profileId, points) {
    const authorProfilePointsRef = admin.database().ref(`/profiles/${profileId}/points`);
    return authorProfilePointsRef.transaction(current => {
          return (current || 0) + points;
    }).then(() => {
        console.log(points, 'points added to user ', profileId);
    });
}

function updateFriendConnection(friend1, friend2, points) {
    const friendConectionRef = admin.database().ref(`/friends/${friend1}/${friend2}/points`);
    return friendConectionRef.transaction(current => {
          return (current || 0) + points;
    }).then(() => {
        console.log('updated friend connection', friend1, friend2, points);
    });
}

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

// exports.ratingPoints = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onWrite(event => {
//     if (event.data.exists() && event.data.previous.exists()) {
//         return console.log("no points for rating updates");
//     }
//     console.log('Points for new/remove rating');

//     const ratingAuthorId = event.params.authorId;
//     const postId = event.params.postId;
//     var point = 1;
//     if (!event.data.exists()) {
//         point = -1;
//         const rating = event.data.previous.val();
//         if (rating.detailedText && rating.rating > 5 && rating.rating <= 15) {
//             point = -2;
//         }
//     }
//     return updateUserRatingPoints(ratingAuthorId, point);
// });

exports.addRatingPoints = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onCreate(event => {
    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;
    var point = 1;
    return updateUserRatingPoints(ratingAuthorId, point);
});

exports.updateFriendConnectionRating = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onCreate(event => {
    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;
    var point = 1;
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {
        var postAuthorId = post.val().authorId;
        if (ratingAuthorId == postAuthorId) {
            return console.log('User rated own post');
        }
        const friend1Task = updateFriendConnection(ratingAuthorId, postAuthorId, point);
        const friend2Task = updateFriendConnection(postAuthorId, ratingAuthorId, point);
        return Promise.all([friend1Task, friend2Task]).then(results => {
            console.log("friend connection tasks on rating completed");
        });
    });
});

exports.updateFriendConnectionComment = functions.database.ref('/post-comments/{postId}/{commentId}/authorId').onCreate(event => {
    const commentAuthorId = event.data.val();
    const postId = event.params.postId;
    const points = 2;

    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {
        var postAuthorId = post.val().authorId;
        if (commentAuthorId == postAuthorId) {
            return console.log('User commented on own post');
        }
        const friend1Task = updateFriendConnection(commentAuthorId, postAuthorId, points);
        const friend2Task = updateFriendConnection(postAuthorId, commentAuthorId, points);
        return Promise.all([friend1Task, friend2Task]).then(results => {
            console.log("friend connection tasks on comment completed");
        });
    });
});

exports.deleteRatingPoints = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onDelete(event => {
    const ratingAuthorId = event.params.authorId;
    const postId = event.params.postId;
    var point = -1;
    const rating = event.data.previous.val();
    if (rating.detailedText && rating.rating > 5 && rating.rating <= 15) {
        point = -2;
    }
    return updateUserRatingPoints(ratingAuthorId, point);
});

function updateUserRatingPoints(ratingAuthorId, point) {
    // Get user points ref
    const userPointsRef = admin.database().ref(`/user-points/${ratingAuthorId}`);
    var newPointRef = userPointsRef.push();
    newPointRef.set({
        'action': point > 0 ? "add":"remove",
        'type': 'rating',
        'value': point,
        'creationDate': admin.database.ServerValue.TIMESTAMP
    });
    // Get rating author.
    const authorProfileRef = admin.database().ref(`/profiles/${ratingAuthorId}`);
    return authorProfileRef.transaction(current => {
        if (current == null) {
            console.log("ignore: null object returned from cache, expect another event with fresh server value.");
            return false;
        }
        current.points = (current.points || 0) + point;
        if (point > 0) {
            current.ratingCount = (current.ratingCount || 0) + 1;
        } else {
            current.ratingCount = (current.ratingCount || 0) - 1;
        }
        return current;
    }).then(() => {
        console.log('User rating points updated.');
    });
}

function updateUserReputationPoints(authorId, points, postId, isUpdate) {
    // Get rated post.
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    const newNotificationTask =  getPostTask.then(post => {
        const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
        var newNotificationRef = userNotificationsRef.push();
        var msg;
        if (isUpdate) {
            // const change = points > 0 ? "incremented" : "decremented";
            msg = `Admin has incremented reputation points by ${Math.abs(points)} for your feedback on "${post.val().title}" recording.`;
        } else {
            msg = `You have been awarded +${points} reputation points by the admin for your feedback on "${post.val().title}" recording.`;
        }
        return newNotificationRef.set({
            'action': 'com.eriyaz.social.activities.PostDetailsActivity',
            'fromUserId' : WELCOME_ADMIN,
            'message': msg,
            'extraKey' : 'PostDetailsActivity.POST_ID_EXTRA_KEY',
            'extraKeyValue' : postId,
            'createdDate': admin.database.ServerValue.TIMESTAMP
        });
    });

    const authorProfileRef = admin.database().ref(`/profiles/${authorId}`);
    const profilePointTask = authorProfileRef.transaction(current => {
        if (current == null) {
            console.log("ignore: null object returned from cache, expect another event with fresh server value.");
            return false;
        }
        current.reputationPoints = (current.reputationPoints || 0) + points;
        return current;
    }).then(() => {
        console.log('User rating points updated.');
    });
    var promises = [newNotificationTask, profilePointTask];
    return Promise.all(promises).then(results => {
        console.log("task completed");
    });
}

function addCommentLikeUser(commentId, profile) {
    console.log("add comment like user", profile.id)
    const likeUserRef = admin.database().ref(`/like-user/${commentId}/${profile.id}`);
    const photoUrl = (profile.photoUrl || "");
    return likeUserRef.set({
        'photoUrl': photoUrl,
        'username': profile.username,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

exports.deleteCommentLikes = functions.database.ref('/comment-likes/{authorId}/{postId}/{commentId}').onDelete(event => {
    const likeAuthorId = event.params.authorId;
    const commentId = event.params.commentId;
    console.log("delete comment like", commentId, likeAuthorId);
    const likeUserRef = admin.database().ref(`/like-user/${commentId}/${likeAuthorId}`);
    return likeUserRef.remove();
});

exports.appNotificationLikes = functions.database.ref('/comment-likes/{authorId}/{postId}/{commentId}').onCreate(event => {
    console.log('App notification for new like');

    const likeAuthorId = event.params.authorId;
    const postId = event.params.postId;
    const commentId = event.params.commentId;

    // Get liked comment  /post-comments/{postId}/{commentId}
    return admin.database().ref(`/post-comments/${postId}/${commentId}`).once('value').then(comment => {
        var commentAuthorId = comment.val().authorId;
        return admin.database().ref(`/posts/${postId}`).once('value').then(post => {
            // Get rating author.
            return admin.database().ref(`/profiles/${likeAuthorId}`).once('value').then(profile => {
                if (likeAuthorId == commentAuthorId) {
                    console.log('no notification: user liked own comment');
                    return addCommentLikeUser(commentId, profile.val());
                } else {
                    addCommentLikeUser(commentId, profile.val());
                }
                // Get user notification
                return admin.database().ref(`/user-notifications/${commentAuthorId}/${commentId}`).once('value').then(notificationSnap => {
                    if (notificationSnap.exists()) {
                        const userNotificationsRef = admin.database().ref(`/user-notifications/${commentAuthorId}/${commentId}`).transaction(current => {
                            if (current != null) {
                                const count = (current.count || 1);
                                const msg = `${profile.val().username} and ${count} more loved your comment on post '${post.val().title}'`;
                                current.count = (current.count || 0) + 1;
                                current.createdDate = admin.database.ServerValue.TIMESTAMP;
                                current.fromUserId = likeAuthorId;
                                current.message = msg;
                                current.read = false;
                            } else {
                                console.log('cache object null for notification ');
                                return false;
                            }
                            return current;
                        }).then(() => {
                            console.log('like app notification updated.');
                        });
                    } else {
                        const msg = `${profile.val().username} loved your comment on post '${post.val().title}'`;
                        return sendAppNotificationPostAction(commentAuthorId, likeAuthorId, msg, commentId, postId);
                    }
                });
            });
        });
    });
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
            // Get user notification
            const userNotificationsTask = admin.database().ref(`/user-notifications/${postAuthorId}/${postId}`+`-rate`).once('value');
            return userNotificationsTask.then(notificationSnap => {
                if (notificationSnap.exists()) {
                    const userNotificationsRef = admin.database().ref(`/user-notifications/${postAuthorId}/${postId}`+`-rate`).transaction(current => {
                        if (current != null) {
                            const count = (current.count || 1);
                            const msg = `${profile.val().username} and ${count} more rated your post '${post.val().title}'`;
                            current.count = (current.count || 0) + 1;
                            current.createdDate = admin.database.ServerValue.TIMESTAMP;
                            current.fromUserId = ratingAuthorId;
                            current.message = msg;
                            current.read = false;
                        } else {
                            console.log('cache object null for notification ');
                            return false;
                        }
                        return current;
                    }).then(() => {
                        console.log('rating app notification updated.');
                    });
                } else {
                    const msg = `${profile.val().username} rated your post '${post.val().title}'`;
                    return sendAppNotificationPostAction(postAuthorId, ratingAuthorId, msg, postId + '-rate', postId);
                }
            });
        });
    });
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
            // Get user notification
            const userNotificationsTask = admin.database().ref(`/user-notifications/${postAuthorId}/${postId}`+`-comment`).once('value');
            return userNotificationsTask.then(notificationSnap => {
                if (notificationSnap.exists()) {
                    const userNotificationsRef = admin.database().ref(`/user-notifications/${postAuthorId}/${postId}`+`-comment`).transaction(current => {
                        if (current != null) {
                            const count = (current.count || 1);
                            const msg = `${profile.val().username} and ${count} more commented on your post '${post.val().title}'`;
                            current.count = (current.count || 0) + 1;
                            current.createdDate = admin.database.ServerValue.TIMESTAMP;
                            current.fromUserId = commentAuthorId;
                            current.message = msg;
                            current.read = false;
                        } else {
                            console.log('cache object null for notification ');
                            return false;
                        }
                        return current;
                    }).then(() => {
                        console.log('comment app notification updated.');
                    });
                } else {
                    const msg = `${profile.val().username} commented on your post '${post.val().title}'`;
                    return sendAppNotificationPostAction(postAuthorId, commentAuthorId, msg, postId + '-comment', postId);
                }
            });
        });
    })
});

exports.normalizeRating = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}/rating').onWrite(event => {
    console.log("calculate normalizeRating");
    // todo: exit for delete
    if (!event.data.exists()) {
        console.log("exit: rating removed");
        return 0;
    }
    const raterId = event.params.authorId;
    const postId = event.params.postId;
    const rating = event.data.val();
    const ratingId = event.params.ratingId;
    const benchmarkRating = 10;
    return avgRatingLastX(raterId, rating).then(avgRating => {
        console.log("avgRating", avgRating);
        let normalRating;
        if (avgRating < benchmarkRating) {
            normalRating = Math.round(rating*(benchmarkRating/avgRating));
            normalRating = Math.min(normalRating, 20);
        } else {
            normalRating = rating;
        }
        return admin.database().ref(`/post-ratings/${postId}/${raterId}/${ratingId}`).transaction(current => {
            if (current == null) {
                console.log("ignore: null object returned from cache, expect another event with fresh server value.");
                return null;
            }
            current.normalizedRating = normalRating;
            return current;
        }).then(() => {
            console.log('normalized rating updated.');
        });
    });
});

function avgRatingLastX(raterId, rating) {
    console.log("calculate avgRating", rating, raterId);
    const maxRatingNum = 10;
    return admin.database().ref(`/user-ratings/${raterId}`).orderByChild("createdDate").limitToLast(maxRatingNum).once('value').then(ratingListSnap => {
        let totalRatingVal = 0;
        let count = 0;
        ratingListSnap.forEach(function(ratingSnap) {
            totalRatingVal = totalRatingVal + ratingSnap.val().rating;
            count++;
            console.log("last Rating", ratingSnap.val().rating);
        })
        console.log("total rating value", totalRatingVal, count);
        if (count > 0) {
            return totalRatingVal/count;
        } else {
            // first rating
            return rating;
        }
    });

}


exports.duplicateUserRating = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}/normalizedRating').onWrite(event => {
    console.log('Duplicate user rating');
    const ratingAuthorId = event.params.authorId;
    const ratingId = event.params.ratingId;
    const postId = event.params.postId;
    return event.data.ref.parent.once("value").then(ratingSnap => {
        const rating = ratingSnap.val();
        if (rating != null) rating.postId = postId;
        const userRatingRef = admin.database().ref(`/user-ratings/${ratingAuthorId}/${ratingId}`);
        return userRatingRef.set(rating);
    });
});

exports.enqueueSupportingRatingTask = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onCreate(event => {
    console.log('enqueue supporting rating tasks');
    const ratingAuthorId = event.params.authorId;
    const rating = event.data.val();
    const postId = event.params.postId;

    if (supportingAuthorIds) {
        return enqueueSupportRating(ratingAuthorId, rating, postId);
    } else {
        return fetchSupportingAuthors().then(result => {
            return enqueueSupportRating(ratingAuthorId, rating, postId);
        })
    }
});

function enqueueSupportRating(ratingAuthorId, rating, postId) {
    // Get author, check if master rater
    if(masterAuthorIds.indexOf(ratingAuthorId) < 0 || rating.rating < 8) {
        console.log("exit: user is not a master rater");
        return 0;
    }

    const ratingCount = random(MAX_SUPPORTING_RATINGS, MIN_SUPPORTING_RATINGS);
    var indexRatingAuthors = randomGroup(ratingCount, supportingAuthorIds.length);
    var promises = [];
    for (var i = 0; i < indexRatingAuthors.length; i++) {
        const authorId = supportingAuthorIds[indexRatingAuthors[i]];
        let randomRating = random(rating.rating + 1, rating.rating - 1);
        if (randomRating > 20) randomRating = 20;
        if (randomRating < 1) randomRating = 1;
        var randomMinutes = random(MAX_RATING_MINUTE, MIN_RATING_MINUTE);
        const fromNow = minutes(randomMinutes);
        promises.push(scheduleTask(randomRating, authorId, postId, fromNow));
    }
    return Promise.all(promises).then(results => {
        console.log("enqueued tasks");
    });
}

function random(max, min) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomGroup(size, range) {
    var arr = []
    while(arr.length < size){
        var randomnumber = Math.floor(Math.random()*range);
        if(arr.indexOf(randomnumber) > -1) continue;
        arr[arr.length] = randomnumber;
    }
    return arr;
}

exports.voiceCommentAppUpdateNotification = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate(event => {
    const postId = event.params.postId;
    const comment = event.data.val();
    const commentAuthorId = comment.authorId;
    if (!comment.audioPath) {
        console.log("comment has no audio");
        return 0;
    }
    const voiceVersion = 2.9;
    return admin.database().ref(`/posts/${postId}`).once('value').then(post => {
        var postAuthorId = post.val().authorId;
        if (commentAuthorId == postAuthorId) {
            return console.log('User commented on own post');
        }
        const msg = "You have received a voice message on your post, but not visible on old versions of app. Tap to Update."
        return notifyAppUpdate(voiceVersion, postAuthorId, msg);
    });
});

function notifyAppUpdate(featureVersion, postAuthorId, msg) {
    return admin.database().ref(`/profiles/${postAuthorId}/appVersion`).once('value').then(versionSnap => {
        const versionStr = versionSnap.val();
        if (!versionStr) return console.log("app version not set");
        const version = parseFloat(versionStr);
        console.log("user app version", version);
        if (version < featureVersion) {
            return sendAppUpdateNotification(postAuthorId, msg);
        }
    });
}

exports.appNotificationFlag = functions.database.ref('/flags/{flaggedUser}/{flagId}').onCreate(event => {
    console.log('App notification for new flag');

    const flagId = event.params.flagId;
    const flag = event.data.val();
    const flaggedUser = event.params.flaggedUser;
    const flaggedBy = flag.flaggedBy;
    var reason = flag.reason;

    const getFlaggedProfileTask = admin.database().ref(`/profiles/${flaggedUser}`).once('value');
    const getFlaggedByProfileTask = admin.database().ref(`/profiles/${flaggedBy}`).once('value');

    return Promise.all([getFlaggedProfileTask, getFlaggedByProfileTask]).then(results => {
        const flaggedSnap = results[0];
        const flaggedBySnap = results[1];
        const flaggedName = flaggedSnap.val().username;
        const flaggedByName = flaggedBySnap.val().username;
        const reportVersion = 3.2;
        const userVersion = flaggedBySnap.val().appVersion;
        if (userVersion < reportVersion) {
            // don't show text message
            reason = "low ratings to everyone";
        }
        var emailMsg = `To ${flaggedName},\n${reason} \n From ${flaggedByName}`;
        console.log("emailMsg for blocked user.", emailMsg);
        const notificationTask = sendComplainAppNotification(flaggedUser, reason);
        const emailTask = sendEmail("RateMySinging: New user complaint", emailMsg);
        return Promise.all([notificationTask, emailTask]).then(results => {
            console.log("all complain task completed");
        });
    });
});

function sendComplainAppNotification(flaggedUser, reason) {
    const userNotificationsRef = admin.database().ref(`/user-notifications/${flaggedUser}`);
    var newNotificationRef = userNotificationsRef.push();
    var msg = `RateMySinging: Received following complaint against you. "${reason}". Nothing to worry, but if too many complaints then we may need to take action.`;
    return newNotificationRef.set({
        'fromSystem' : true,
        'message': msg,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

exports.appNotificationBlock = functions.database.ref('/block-users/{blockedUser}/{blockedBy}').onCreate(event => {
    console.log('App notification for new block');

    const blockedBy = event.params.blockedBy;
    const blockedUser = event.params.blockedUser;
    const reason = event.data.val().reason;
    return admin.database().ref(`profiles/${blockedBy}`).once('value').then(function(profileSnap) {
        var profile = profileSnap.val();
        const msg = `${profile.username} has blocked you. You cannot rate, comment or message him/her in future.`;
        const notificationTask = sendAppNotificationProfileAction(blockedUser, blockedBy, msg);
        return admin.database().ref(`profiles/${blockedUser}`).once('value').then(function(profileBlockedSnap) {
            var emailMsg = `Blocked ${profileBlockedSnap.val().username}, \n${reason} \n By ${profile.username}`;
            return sendEmail("RateMySinging: Block user", emailMsg);
        });
    });
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
        if (postAuthorId != commentAuthorId) return console.log("no notification if comment not from post author");
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

exports.appNotificationMessages = functions.database.ref('/user-messages/{userId}/{messageId}').onCreate(event => {
    console.log('App notification for new message');

    const messageId = event.params.messageId;
    const userId = event.params.userId;
    const message = event.data.val();
    const messageAuthorId = message.senderId;
    const messageListRef = event.data.ref.parent;
    const parentMessageId = message.parentId;

    if (parentMessageId == null) {
        if (messageAuthorId == userId) {
            return console.log("messaged on own profile, no notification");
        }
         // Get message author.
        const getMessageAuthorProfileTask = admin.database().ref(`/profiles/${messageAuthorId}`).once('value');
        return getMessageAuthorProfileTask.then(profile => {
            var msg = profile.val().username + " left a message on your profile page.";
            return sendChatPushAppNotification(userId, messageAuthorId, msg, userId);
        });
    }
    // Get parent feedback.
    const getParentMessageTask = admin.database().ref(`/user-messages/${userId}/${parentMessageId}`).once('value');

    return getParentMessageTask.then(parentMessage => {
        const parentMessageVal = parentMessage.val();
        console.log("new reply on message ", parentMessageVal.id);
        var parentMessageAuthorId = parentMessageVal.senderId;
        var sentParentAuthorNotification = false;
        if (parentMessageAuthorId != messageAuthorId) sentParentAuthorNotification = true;

        // Get all message with same parent message
        const getChildrenMessageTask = messageListRef.orderByChild('parentId').equalTo(parentMessageId).once('value');
        return getChildrenMessageTask.then(snapshot => {
            const authorToNotify = [];
            snapshot.forEach(function(messageSnap) {
                // exclude parent author & message author
                const notifyAuthorId = messageSnap.val().senderId;
                if (notifyAuthorId  != parentMessageAuthorId && notifyAuthorId != messageAuthorId ) {
                    if (!authorToNotify.includes(notifyAuthorId)) {
                        authorToNotify.push(notifyAuthorId);
                    }
                }
            });
            if (authorToNotify.length == 0 && sentParentAuthorNotification == false) return;
            // Get message author.
            const getMessageAuthorProfileTask = admin.database().ref(`/profiles/${messageAuthorId}`).once('value');
            return getMessageAuthorProfileTask.then(profile => {
                const promisePool = new PromisePool(() => {
                    if (sentParentAuthorNotification) {
                        sentParentAuthorNotification = false;
                        var msg = profile.val().username + " replied on your message.";
                        return sendChatPushAppNotification(parentMessageAuthorId, messageAuthorId, msg, userId);
                    }
                  if (authorToNotify.length > 0) {
                    const authorId = authorToNotify.pop();
                    var msg = profile.val().username + " replied on the message on which you also replied.";
                    return sendAppMessageNotification(authorId, messageAuthorId, msg, userId);
                  }
                  return null;
                }, MAX_CONCURRENT);
                const poolTask =  promisePool.start();
                return poolTask.then(() => {
                    return console.log('sent message notification task completed.');
                });
            });
        });
    });
});

function sendChatPushAppNotification(authorId, fromUserId, msg, extraKeyValue) {
    var promises = [];
    promises.push(sendChatPushNotification(fromUserId, authorId, msg, "MESSAGE_ACTIVITY", extraKeyValue));
    promises.push(sendAppMessageNotification(authorId, fromUserId, msg, extraKeyValue));
    return Promise.all(promises).then(results => {
        console.log("chat push notification task completed");
    });
}

function sendAppMessageNotification(authorId, fromUserId, msg, extraKeyValue) {
    console.log("sending msg:", msg);
    // Get user notification ref
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
    var newNotificationRef = userNotificationsRef.push();
    return newNotificationRef.set({
        'action': 'com.eriyaz.social.activities.MessageActivity',
        'fromUserId' : fromUserId,
        'message': msg,
        'extraKey' : 'ProfileActivity.USER_ID_EXTRA_KEY',
        'extraKeyValue' : extraKeyValue,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

function sendFeedbackPushAppNotification(authorId, fromUserId, msg) {
    var promises = [];
    promises.push(sendChatPushNotification(fromUserId, authorId, msg, "FEEDBACK_ACTIVITY", ""));
    promises.push(sendAppFeedbackNotification(authorId, fromUserId, msg));
    return Promise.all(promises).then(results => {
        console.log("feedback push notification task completed");
    });
}

function sendAppFeedbackNotification(authorId, fromUserId, msg) {
    // Get user notification ref
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
    var newNotificationRef = userNotificationsRef.push();
    return newNotificationRef.set({
            'action': 'com.eriyaz.social.activities.FeedbackActivity',
            'fromUserId' : fromUserId,
            'message': msg,
            'createdDate': admin.database.ServerValue.TIMESTAMP
        }).then(() => {
            console.log('sent new feedback reply notification on feedback you replied to ', authorId);
        });
}


function sendAppNotificationProfileAction(authorId, fromUserId, msg) {
    console.log("sending profile action notification:", msg);
    // Get user notification ref
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
    var newNotificationRef = userNotificationsRef.push();
    return newNotificationRef.set({
        'action': 'com.eriyaz.social.activities.ProfileActivity',
        'fromUserId' : fromUserId,
        'message': msg,
        'extraKey' : 'ProfileActivity.USER_ID_EXTRA_KEY',
        'extraKeyValue' : fromUserId,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

function sendAppNotificationPostAction(authorId, fromUserId, msg, notificationId, postId) {
    console.log("sending profile action notification:", msg);
    // Get user notification ref
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}/${notificationId}`);
    return userNotificationsRef.set({
                'action': 'com.eriyaz.social.activities.PostDetailsActivity',
                'fromUserId' : fromUserId,
                'message': msg,
                'count' : 1,
                'extraKey' : 'PostDetailsActivity.POST_ID_EXTRA_KEY',
                'extraKeyValue' : postId,
                'createdDate': admin.database.ServerValue.TIMESTAMP
            });
}

function sendAppUpdateNotification(authorId, msg) {
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
    var newNotificationRef = userNotificationsRef.push();
    // add notification
    return newNotificationRef.set({
        'action': 'com.eriyaz.social',
        'message': msg,
        'openPlayStore': true,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}


function sendAppNotificationNoAction(authorId, fromUserId, msg) {
    console.log("sending app notification:", msg);
    // Get user notification ref
    const userNotificationsRef = admin.database().ref(`/user-notifications/${authorId}`);
    var newNotificationRef = userNotificationsRef.push();
    return newNotificationRef.set({
        'fromUserId' : fromUserId,
        'message': msg,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

function sendUserMessage(authorId, fromUserId, msg) {
    // Get user notification ref
    const userMessagesRef = admin.database().ref(`/user-messages/${authorId}`);
    var newUserMsgRef = userMessagesRef.push();
    return newUserMsgRef.set({
        'id': newUserMsgRef.key,
        'receiverId': authorId,
        'senderId' : fromUserId,
        'text': msg,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    });
}

exports.appNotificationFeedbackConversation = functions.database.ref('/feedbacks/{feedbackId}').onCreate(event => {
    console.log('App notification for new feedback in conversation');

    const feedbackListRef = event.data.ref.parent;
    const feedbackId = event.params.feedbackId;
    const feedback = event.data.val();
    const feedbackAuthorId = feedback.senderId;
    const parentFeedbackId = feedback.parentId;

    if (parentFeedbackId == null) {
        console.log("stand alone feedback: notify admin, feedback id", feedbackId);
        const getFeedbackAuthorProfileTask = admin.database().ref(`/profiles/${feedbackAuthorId}`).once('value');
        return getFeedbackAuthorProfileTask.then(profile => {
            var msg = "New feedback by " + profile.val().username + " in Contact Us screen.";
            return sendFeedbackPushAppNotification(WELCOME_ADMIN, feedbackAuthorId, msg);
        });
    }

    // Get parent feedback.
    const getParentFeedbackTask = admin.database().ref(`/feedbacks/${parentFeedbackId}`).once('value');

    return getParentFeedbackTask.then(parentFeedback => {
        const parentFeedbackVal = parentFeedback.val();
        console.log("new reply on feedback ", parentFeedbackVal.id);
        var parentFeedbackAuthorId = parentFeedbackVal.senderId;
        var sentParentAuthorNotification = false;
        if (parentFeedbackAuthorId != feedbackAuthorId) sentParentAuthorNotification = true;

        // Get all feedback with same parent feedback
        const getChildrenFeedbackTask = feedbackListRef.orderByChild('parentId').equalTo(parentFeedbackId).once('value');
        return getChildrenFeedbackTask.then(snapshot => {
            const authorToNotify = [];
            snapshot.forEach(function(feedbackSnap) {
                // exclude parent author & feedback author
                const notifyAuthorId = feedbackSnap.val().senderId;
                if (notifyAuthorId  != parentFeedbackAuthorId && notifyAuthorId != feedbackAuthorId ) {
                    if (!authorToNotify.includes(notifyAuthorId)) {
                        authorToNotify.push(notifyAuthorId);
                    }
                }
            });
            if (authorToNotify.length == 0 && sentParentAuthorNotification == false) return;
            // Get feedback author.
            const getFeedbackAuthorProfileTask = admin.database().ref(`/profiles/${feedbackAuthorId}`).once('value');
            return getFeedbackAuthorProfileTask.then(profile => {
                const promisePool = new PromisePool(() => {
                    if (sentParentAuthorNotification) {
                        sentParentAuthorNotification = false;
                        var msg = profile.val().username + " replied on your feedback in Contact Us screen.";
                        return sendFeedbackPushAppNotification(parentFeedbackAuthorId, feedbackAuthorId, msg);
                    }
                  if (authorToNotify.length > 0) {
                    const authorId = authorToNotify.pop();
                    var msg = profile.val().username + " replied on the feedback on which you also replied in Contact Us screen.";
                    return sendFeedbackPushAppNotification(authorId, feedbackAuthorId, msg);
                  }
                  return null;
                }, MAX_CONCURRENT);
                const poolTask =  promisePool.start();
                return poolTask.then(() => {
                    return console.log('sent feedback notification task completed.');
                });
            });
        });
    });
});

exports.incrementUserUnseenNotification = functions.database.ref('/user-notifications/{authorId}/{notificationId}/message').onWrite(event => {
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

exports.generateChecksum = functions.https.onRequest((req, res) => {
    var paramarray = {};
    paramarray['MID'] = paytm_config.MID; //Provided by Paytm
    paramarray['ORDER_ID'] = req.query.orderId; //unique OrderId for every request
    paramarray['CUST_ID'] = req.query.customerId;  // unique customer identifier
    paramarray['INDUSTRY_TYPE_ID'] = paytm_config.INDUSTRY_TYPE_ID; //Provided by Paytm
    paramarray['CHANNEL_ID'] = paytm_config.CHANNEL_ID; //Provided by Paytm
    paramarray['TXN_AMOUNT'] = req.query.txnAmount; // transaction amount
    paramarray['WEBSITE'] = paytm_config.WEBSITE; //Provided by Paytm
    paramarray['CALLBACK_URL'] = paytm_config.CALLBACK_URL+req.query.orderId;//Provided by Paytm
    paytm_checksum.genchecksum(paramarray, paytm_config.MERCHANT_KEY, function (err, output) {
        return res.status(200).send(JSON.stringify(output));
    });
});

exports.verfiyTransactionStatus = functions.https.onRequest((req, res) => {
    var orderId = req.query.orderId;
    var customerId = req.query.customerId;
    var paramarray = {};
    paramarray['MID'] = paytm_config.MID; //Provided by Paytm
    paramarray['ORDER_ID'] = orderId; //unique OrderId for every request

    paytm_checksum.genchecksum(paramarray, paytm_config.MERCHANT_KEY, function (err, output) {
        console.log("checksum output", JSON.stringify(output));

        var options = {
          host: 'securegw.paytm.in',// https://securegw-stage.paytm.in
          port: 443,
          path: '/merchant-status/getTxnStatus?JsonData='+JSON.stringify(output),
          method: 'GET',
        };

        var paytmReq = https.request(options, function(paytmRes) {
          paytmRes.setEncoding('utf8');
          paytmRes.on('data', function (chunk) {
            var resJson = JSON.parse(chunk);
            res.status(200).send(chunk);
            return createOrUpdateBoughtFeedback(orderId, customerId, resJson.STATUS);
          });
        });

        paytmReq.on('error', function(e) {
          console.log("Got error: " + e);
            return res.status(200).send("Got error "+e);
        });
        paytmReq.end();
    });

});

function createOrUpdateBoughtFeedback(postId, authorId, paymentStatus) {
    console.log("createOrUpdateBoughtFeedback", postId, authorId, paymentStatus);
    // Get user notification ref
    const boughtFeedbackRef = admin.database().ref(`/bought-feedbacks/${postId}`);
    boughtFeedbackRef.once('value').then(feedbackSnap => {
        var feedback = feedbackSnap.val();
        if (feedback) {
            // just update status
            const paymentStatuskRef = admin.database().ref(`/bought-feedbacks/${postId}/paymentStatus`);
            return paymentStatuskRef.transaction(current => {
                  return paymentStatus;
            }).then(() => {
                console.log('payment status updated to', paymentStatus);
            });
        } else {
            console.log("created new bought feedback");
            return boughtFeedbackRef.set({
                'postId': postId,
                'authorId' : authorId,
                'paymentStatus': paymentStatus,
                'createdDate': admin.database.ServerValue.TIMESTAMP
            });
        }

    });
}

exports.restoreReputationPoints = functions.database.ref('/profiles/{uid}/reputationPoints').onDelete(event => {
    var uid = event.params.uid;
    console.log("restore lost reputation points", uid);
    const previousPoints = event.data.previous.val();
    if (!previousPoints) return console.log("exit: previous reputationPoints null");
    const profileReputationPointsRef = admin.database().ref(`profiles/${uid}/reputationPoints`);
    return profileReputationPointsRef.transaction(current => {
          return previousPoints;
    }).then(() => {
        console.log('restored reputationPoints completed for profile', uid);
    });
});

exports.grantSignupReward = functions.database.ref('/profiles/{uid}/id').onCreate(event => {
    console.log("new user signed in");
    var uid = event.params.uid;
    return admin.database().ref(`profiles/${uid}`).once('value').then(function(profileSnap) {
          var profile = profileSnap.val();
          console.log("referred_by", profile.referred_by);
          const WELCOME_MSG = `Hi ${profile.username}. Welcome to ${notificationTitle} community. I am the Developer and a Moderator here. Feel free to reach out to me for any questions/help. \nImportant : Be fair and genuine in your ratings, or you might be blacklisted by other members.`;
          const GIFT_JOINING_MSG = "Vikas Patel gifted you 4 points. Please rate others' recordings to earn more points.";
          const welcomeMsgTask = sendUserMessage(uid, WELCOME_ADMIN, WELCOME_MSG);
          // const addPointsTask =  addPoints(uid, 3);
          const giftJoiningMsgTask = sendAppNotificationNoAction(uid, WELCOME_ADMIN, GIFT_JOINING_MSG);
          const taskList = [welcomeMsgTask, giftJoiningMsgTask];
          if (profile.referred_by) {
            // add reward points
            // const addPointsTask =  addPoints(profile.referred_by, REWARD_POINTS);
            const msg = "Congrats your friend " +  profile.username + " joined.";
            const notificationTask = sendAppNotificationProfileAction(profile.referred_by, uid, msg);
            taskList.push(notificationTask);
          }
        return Promise.all(taskList).then(results => {
            console.log("all reward tasks completed.");
        });
    });
});

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
            return sendAppUpdateNotification(authorId, msg);
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

exports.appUninstall = functions.analytics.event('app_remove').onLog(event => {
    const user = event.data.user;
    const uid = user.userId;
    console.log("app uninstall detected for uid ",uid);
    // Get profile of user and set user details
    if (uid) {
        admin.database().ref(`/profiles/${uid}`).once('value').then(snapshot => {
            const profile = snapshot.val();
            if (profile.phone) {
                console.log("uninstall user with phone", profile);
                sendEmail("RateMySinging: unInstalled User", JSON.stringify(profile));
            }

        });
        const profileUninstallRef = admin.database().ref(`profiles/${uid}/unInstalled`);
        return profileUninstallRef.transaction(current => {
              return true;
        }).then(() => {
            console.log('mark uninstall for profile ', uid);
            return markPostRemoved(uid, true);
        });
    } else {
        return 0;
    }
});

exports.sessionStart = functions.analytics.event('session_start').onLog(event => {
    const user = event.data.user;
    const uid = user.userId;
    const appVersion = user.appInfo.appVersion;
    if (uid) {
        const profileAppVersionRef = admin.database().ref(`profiles/${uid}/appVersion`);
        return profileAppVersionRef.transaction(current => {
              return appVersion;
        }).then(() => {
            console.log(`app version ${appVersion} set for profile ${uid}`);
        });
    } else {
        return 0;
    }
});

exports.userReInstall = functions.analytics.event('SignIn').onLog(event => {
    const user = event.data.user;
    const uid = user.userId;
    console.log("signin detected for user ", uid);
    return admin.database().ref(`profiles/${uid}/unInstalled`).once('value').then(function(data) {
      var isUnInstalled = data.val();
      if (isUnInstalled) {
        const profileUninstallRef = admin.database().ref(`profiles/${uid}/unInstalled`);
        return profileUninstallRef.transaction(current => {
              return false;
        }).then(() => {
            console.log('mark install for profile ', uid);
            return markPostRemoved(uid, false);
        });
      }
    });
});

function markPostRemoved(uid, isRemoved) {
    console.log("mark posts removed", isRemoved, "for user", uid);
    const postRef = admin.database().ref(`/posts`);
    const postQuery = postRef.orderByChild('authorId').equalTo(uid).once('value');
    return postQuery.then(userPostsSnap => {
        var updatePosts = {};
        userPostsSnap.forEach(function(postSnap) {
            // exclude parent author & message author
            const postId = postSnap.key;
            updatePosts['/' + postId + '/removed'] = isRemoved;
        });
        return postRef.update(updatePosts).then(() => {
            console.log("update posts", updatePosts);
        });
    });
}

function fetchSupportingAuthors() {
    const profileRef = admin.database().ref(`/profiles`);
    const profileQuery = profileRef.orderByChild('support').equalTo(true).once('value');
    supportingAuthorIds = [];
    return profileQuery.then(authorsSnap => {
        authorsSnap.forEach(function(authorSnap) {
            supportingAuthorIds.push(authorSnap.key);
        });
        console.log("fetchSupportingAuthors", supportingAuthorIds);
        masterAuthorIds = [];
        return profileRef.orderByChild('master').equalTo(true).once('value').then(authorsSnap => {
            authorsSnap.forEach(function(authorSnap) {
                masterAuthorIds.push(authorSnap.key);
            });
            return console.log("fetched master authors", masterAuthorIds);
        });
    });
}

/// WORKERS ///

const workers = {
    taskSupportingRatings
}

function taskSupportingRatings(task) {
    console.log('worker task executed')
    return createRating(task.rating, task.authorId, task.postId);
}

function scheduleTask(ratingVal, authorId, postId, fromNow) {
    console.log("schedule task", ratingVal, authorId);
    const queueRef = db.ref('tasks');
    var newTaskRef = queueRef.push();
    return newTaskRef.set({
        'authorId': authorId,
        'rating': ratingVal,
        'postId': postId,
        'worker': 'taskSupportingRatings',
        'time': Date.now() + fromNow
    });
}

function createRating(ratingVal, authorId, postId) {
    var ratingAuthorRef = db.ref(`/post-ratings/${postId}/${authorId}`);
    var ratingRef = ratingAuthorRef.push();
    return ratingRef.set({
        'id': ratingRef.key,
        'authorId': authorId,
        'rating': ratingVal,
        'createdDate': admin.database.ServerValue.TIMESTAMP
    })
}

exports.profileSearch = functions.https.onRequest((req, res) => {
    const queryText = req.query.name;
    if (!queryText) return res.status(200).send(`name parameter null`);
    console.log("profile search by", queryText);
    const queueRef = db.ref('profiles');

    // Get all tasks that with expired times
    return queueRef.orderByChild('username').startAt(queryText).endAt(queryText+"\uf8ff").once('value').then(profiles => {
        if (profiles.exists()) {
            let arr = [];
            profiles.forEach( profileSnap => {
                const profile = profileSnap.val();
                arr.push(JSON.stringify(profile));
            })

            res.status(200).send(arr.join('\n'));

        } else {

            res.status(200).send(`No result found`);
        }

    });
});

// function removeOldPostsFromCache() {
//     while(recentPosts[recentPosts.length-1].createdDate < Date.now() - cacheDays){
//         recentPosts.pop();
//     }
// }

// exports.updatePostCache = functions.database.ref('/posts/{postId}').onWrite(event => {
//     const postId = event.params.postId;
//     console.log("recentPosts", recentPosts);
//     if (!recentPosts) {
//         console.log("empty cache");
//         return 0;
//     }
//     // Add
//     if (event.data.exists() && !event.data.previous.exists()) {
//         const post = event.data.val();
//         recentPosts.unshift(post);
//         removeOldPostsFromCache();
//         console.log("new post created");
//         return 0;
//     }

//     // Update
//     if (event.data.exists() && event.data.previous.exists()) {
//         const post = event.data.val();
//         if (post.createdDate < Date.now() - cacheDays) return console.log("no need to update the cache");
//         for( var i = 0; i < recentPosts.length-1; i++){
//            if (recentPosts[i].id == postId) {
//              recentPosts.splice(i, 1, post);
//            }
//         }
//         console.log("post updated");
//         return 0;
//     }

//     // Delete
//     if (!event.data.exists()) {
//         const post = event.data.previous.val();
//         if (post.createdDate < Date.now() - cacheDays) return console.log("no need to delete from the cache");
//         for( var i = 0; i < recentPosts.length-1; i++){
//            if (recentPosts[i].id == postId) {
//              recentPosts.splice(i, 1);
//            }
//         }
//         console.log("post deleted from cache", postId);
//         return 0;
//     }

// });

exports.postList = functions.https.onCall((data, context) => {
    const lastRecentDate = data.lastRecentDate;
    const lastFriendDate = data.lastFriendDate;
    console.log("lastRecentDate, lastFriendDate", lastRecentDate, lastFriendDate);
    return getRecentPostList().then(postList => {
        if (!context.auth || !context.auth.uid) {
            return filterPostList(postList, lastRecentDate, lastFriendDate);
        }
        const uid = context.auth.uid;
        let yesterday = Date.now() - cacheDays;
        return Promise.all([getFriends(uid), getRatedPosts(uid, yesterday)]).then(results => {
            const friends = results[0];
            const ratedPosts = results[1];
            return filterPostList(postList, lastRecentDate, lastFriendDate, friends, ratedPosts, uid);
        });
    });
});

function filterPostList(recentPosts, lastRecentDate = Date.now(), lastFriendDate = Date.now(), friends = [], ratedPosts = [], uid) {
    const result_size = 10;
    let friendPosts = [];
    let newPosts = [];
    let resultPosts = [];

    newPosts = recentPosts.filter(post => {
        if (post.createdDate <= lastRecentDate && !friends.includes(post.authorId)) return true;
        return false;
    });

    friendPosts = recentPosts.filter(post => {
        if (post.createdDate <= lastFriendDate && friends.includes(post.authorId) && !ratedPosts.includes(post.id)) return true;
        return false;
    });

    for (var i = 0; i < result_size; i++) {
        if (i < newPosts.length) {
            resultPosts.push(newPosts[i]);
            if (newPosts[i].createdDate < lastRecentDate) lastRecentDate = newPosts[i].createdDate;
        }
        if (i < friendPosts.length) {
            resultPosts.push(friendPosts[i]);
            if (friendPosts[i].createdDate < lastFriendDate) lastFriendDate = friendPosts[i].createdDate;
        }
        if (resultPosts.length >= result_size) break;
    }
    if (resultPosts.length >= result_size) {
        return {
          lastRecentDate: lastRecentDate,
          lastFriendDate: lastFriendDate,
          result: resultPosts
        };
    } else {
        if (uid) {
            return getRatedPosts(uid).then(ratedPostsAll => {
                lastRecentDate = Math.min(lastRecentDate, lastFriendDate);
                return getYesterdayFilteredPostList(resultPosts, ratedPostsAll, lastRecentDate, lastFriendDate, result_size);
            });
        } else {
            return getYesterdayFilteredPostList(resultPosts, [], lastRecentDate, lastFriendDate, result_size);
        }
    }
}

function getYesterdayFilteredPostList(resultList, ratedPostList, lastRecentDate, lastFriendDate, limit) {
    // get yesterday post list
    // filter rated post
    // if more than limit
    // otherwise recursive function call
    return getYesterdayPostList(lastRecentDate, limit).then(yesterdayPosts => {
        if (!yesterdayPosts || yesterdayPosts.length == 0) {
            console.log("end of posts");
            return {
              lastRecentDate: lastRecentDate,
              lastFriendDate: lastFriendDate,
              result: resultList
            };
        }
        lastRecentDate = yesterdayPosts[yesterdayPosts.length - 1].createdDate;
        // filter removed and complained posts
        yesterdayPosts = yesterdayPosts.filter(post => {
            return !post.removed && !post.hasComplain && post.ratingsCount <= 10;
        });
        yesterdayPosts = yesterdayPosts.filter(post => {
            if (!ratedPostList.includes(post.id)) return true;
            return false;
        });
        if (yesterdayPosts.length + resultList.length >= limit) {
            let subArray = yesterdayPosts.slice(0, limit - resultList.length);
            resultList = resultList.concat(subArray);
            lastRecentDate = subArray[subArray.length - 1].createdDate;
            return {
              lastRecentDate: lastRecentDate,
              lastFriendDate: lastFriendDate,
              result: resultList
            };
        }
        resultList = resultList.concat(yesterdayPosts);
        return getYesterdayFilteredPostList(resultList, ratedPostList, lastRecentDate, lastFriendDate, limit);
    });
}

function getYesterdayPostList(lastDate, limit) {
    const postRef = db.ref('posts');
    const yesterdayPosts = [];
    return postRef.orderByChild('createdDate').endAt(lastDate - 1).limitToLast(limit).once('value').then(postListSnap => {
        postListSnap.forEach( postSnap => {
            const post = postSnap.val();
            post.id = postSnap.key;
            yesterdayPosts.push(post);
        });
        yesterdayPosts.reverse();
        return yesterdayPosts;
    });
}

function getRecentPostList() {
    // if (recentPosts) {
    //     console.log("return from results", recentPosts.length);
    //     return Promise.resolve(recentPosts);
    // }
    const postRef = db.ref('posts');
    let yesterday = Date.now() - cacheDays;
    return postRef.orderByChild('createdDate').startAt(yesterday).once('value').then(postListSnap => {
        recentPosts = [];
        postListSnap.forEach( postSnap => {
            const post = postSnap.val();
            post.id = postSnap.key;
            recentPosts.push(post);
        });
        recentPosts = recentPosts.filter(post => {
            return !post.removed && !post.hasComplain && post.ratingsCount <= 10;
        });
        recentPosts.reverse();
        console.log("return after filter", recentPosts.length);
        return recentPosts;
    });
}

function getFriends(userId) {
    let friends = [];
    const friendsRef = db.ref(`/friends/${userId}`);
    return friendsRef.once('value').then(friendListSnap => {
        friendListSnap.forEach(friendSnap => {
            friends.push(friendSnap.key);
        });
        return friends;
    });
}

function getRatedPosts(userId, lastDate) {
    let posts = [];
    const ratedPostsRef = db.ref(`/user-ratings/${userId}`);
    if (lastDate) {
        return ratedPostsRef.orderByChild('createdDate').startAt(lastDate).once('value').then(postListSnap => {
            postListSnap.forEach(postSnap => {
                const post = postSnap.val();
                posts.push(post.postId);
            });
            return posts;
        });
    } else {
        return ratedPostsRef.once('value').then(postListSnap => {
            postListSnap.forEach(postSnap => {
                const post = postSnap.val();
                posts.push(post.postId);
            });
            return posts;
        });
    }
}

exports.profileStats = functions.https.onRequest((req, res) => {
    const queryText = req.query.name;
    if (!queryText) return res.status(200).send(`name parameter null`);
    console.log("profile search by", queryText);
    const queueRef = db.ref('profiles');

    // Get all tasks that with expired times
    return queueRef.orderByChild('appVersion').equalTo(queryText).once('value').then(profiles => {
        if (profiles.exists()) {
            let totalProfileCount = 0;
            let uninstallProfileCount = 0;
            profiles.forEach( profileSnap => {
                totalProfileCount++;
                const profile = profileSnap.val();
                if (profile.unInstalled) {
                    uninstallProfileCount++;
                }
            })
            res.status(200).send("Total profile count: " + totalProfileCount + " \nUninstall profile count: " + uninstallProfileCount);

        } else {

            res.status(200).send(`No result found`);
        }

    });
});

exports.rankTaskRunner = functions.https.onRequest((req, res) => {
    console.log("rank task runner");
    const queueRef = db.ref('tasks');
    const profileRef = admin.database().ref(`/profiles`);
    var updateProfiles = {};
    let updated = 0;
    let rank = 0;
    var sortOnParam;
    // Get parameter (weeklyPoints = "true") from request URL
    var sortOnWeeklyPoints = req.query.weeklyPoints;
    console.log("Request parameter ", sortOnWeeklyPoints);
    if (sortOnWeeklyPoints != null)
    {
      sortOnParam = "weeklyReputationPoints";
    }
    else
    {
      sortOnParam = "reputationPoints";
    }
    profileRef.orderByChild(sortOnParam).startAt(1).once('value').then((profiles) => {
        rank = profiles.numChildren();
        // return in asc order by reputation points
        profiles.forEach( profileSnap => {
            var key = profileSnap.key;
            if (sortOnParam == "reputationPoints") {
                const previousRank = profileSnap.val().rank;
                if (!previousRank || previousRank != rank)
                    updateProfiles[`${key}/rank`] = rank;
            }
            else
            {
                const previousRank1 = profileSnap.val().weeklyRank;
                if (!previousRank1 || previousRank1 != rank) {
                    console.log("weeklyReputationPoints", rank);
                    updateProfiles[`${key}/weeklyRank`] = rank;
                }
            }
            updated++;
            rank--;
        });
        console.log("updated profiles", updated);
                res.status(200).send(`updated profiles ${updated}`);
                return profileRef.update(updateProfiles);

        });
    });

exports.weeklyPointsTaskRunner = functions.https.onRequest((req, res) => {
    console.log("Weekly points task runner");
    const profileRef = admin.database().ref("/profiles");
    var updateProfiles = {};
    let updated = 0;

    profileRef.once('value').then((snapshot) => {
        // For each profile, calculate weeklyReputationPoints and update lastweekReputationPoints
	    snapshot.forEach(child => {
		let key = child.key;
		const reputationPoints = child.val().reputationPoints;
		const lastweekReputationPoints = child.val().lastweekReputationPoints;
		const weeklyReputationPoints = child.val().weeklyReputationPoints;

        if (typeof weeklyReputationPoints == 'undefined' ||
            typeof reputationPoints == 'undefined' ||
            typeof lastweekReputationPoints == 'undefined') {

            console.log("unable to update profile with ID ",key);
        }
        else {
            // Update weeklyReputationPoints and lastweekReputationPoints in database
            updateProfiles[`${key}/weeklyReputationPoints`] = reputationPoints - lastweekReputationPoints;
            updateProfiles[`${key}/lastweekReputationPoints`] = reputationPoints;
            updated++;
        }
        });
        console.log("updated profiles", updated);
        res.status(200).send(`updated profiles ${updated}`);
        return profileRef.update(updateProfiles);
    });
});

/// TASK RUNNER CLOUD FUNCTION ///

exports.taskRunner = functions.https.onRequest((req, res) => {
    console.log("task runner");
    const queueRef = db.ref('tasks');

    // Get all tasks that with expired times
    return queueRef.orderByChild('time').endAt(Date.now()).once('value').then(tasks => {
        if (tasks.exists()) {
            const promises = []

            // Execute tasks concurrently
            tasks.forEach( taskSnapshot => {
                promises.push( execute(taskSnapshot) )
            })

            return Promise.all(promises).then(results => {
                // Optional: count success/failure ratio
                const successCount = results.length;

                res.status(200).send(`Work complete. ${successCount} succeeded`);
            });

        } else {

            res.status(200).send(`Task queue empty`);
        }

    });
});

/// HELPERS

// Helper to run the task, then clear it from the queue
function execute(taskSnapshot) {

    const task = taskSnapshot.val();
    const key = taskSnapshot.key;
    const ref = db.ref(`tasks/${key}`);

    try {
        // execute worker for task
        console.log("task", task);
        return workers[task.worker](task).then(result => {
            // If the task has an interval then reschedule it, else remove it
            if (task.interval) {
                return ref.update({
                    time: task.time + task.interval,
                    runs: (task.runs || 0) + 1
                })
            } else {
                return ref.remove();
            }
        });
    } catch(err) {
        // If error, update fail count and error message
        return ref.update({
            err: err.message,
            failures: (task.failures || 0) + 1
        });
    }
}

// Used to count the number o fail
function sum(acc, num) {
    return acc + num;
}

function minutes(value) {
   return value * 60 * 1000;
}

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



// const bigquery = require('@google-cloud/bigquery')();

// exports.syncBigQueryPost = functions.database.ref('/posts/{postId}').onCreate((snapshot,context) => {

// 	const dataset = bigquery.dataset("com_eriyaz_social_ANDROID");
// 	const table = dataset.table("post");

// 	const postTitle = snapshot.val().title;
// 	return table.insert({
// 		id: context.params.postId,
// 		title : postTitle,
// 	  });
// });
