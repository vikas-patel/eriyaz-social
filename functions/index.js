var functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

const actionTypeNewRating = "new_rating"
const actionTypeNewComment = "new_comment"
const actionTypeNewPost = "new_post"
const notificationTitle = "YourSingingScore"

const postsTopic = "postsTopic"

exports.pushNotificationRatings = functions.database.ref('/post-ratings/{postId}/{authorId}/{ratingId}').onWrite(event => {

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

exports.pushNotificationComments = functions.database.ref('/post-comments/{postId}/{commentId}').onWrite(event => {

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
        const updates = {};
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

exports.commentsPoints = functions.database.ref('/post-comments/{postId}/{commentId}').onWrite(event => {

    if (event.data.exists() && event.data.previous.exists()) {
        return console.log("no points for comment updates");
    }
    const commentId = event.params.commentId;
    const postId = event.params.postId;
    const comment = event.data.exists() ? event.data.val() : event.data.previous.val();
    const commentAuthorId = comment.authorId;
    const comment_points = 2;

    console.log('New comment was added, post id: ', postId);

    // Get the commented post .
    const getPostTask = admin.database().ref(`/posts/${postId}`).once('value');

    return getPostTask.then(post => {

        if (commentAuthorId == post.val().authorId) {
            return console.log('User commented on own post');
        }

        // Get user points ref
        const userPointsRef = admin.database().ref(`/user-points/${commentAuthorId}`);
        var newPointRef = userPointsRef.push();
        newPointRef.set({
            'action': event.data.exists() ? "add":"remove",
            'type': 'comment',
            'value': event.data.exists() ? comment_points:-comment_points,
            'creationDate': admin.database.ServerValue.TIMESTAMP
        });

        // Get rating author.
        const authorProfilePointsRef = admin.database().ref(`/profiles/${commentAuthorId}/points`);
        return authorProfilePointsRef.transaction(current => {
            if (event.data.exists()) {
              return (current || 0) + comment_points;
            } else {
              return (current || 0) - comment_points;
            }
        }).then(() => {
            console.log('User comment points updated.');
        });

    })
});

// Two different fuctions for post add and remove, because there were too many post update request
// and firebase has restriction on frequency of function calls.
exports.postAddedPoints = functions.database.ref('/posts/{postId}').onCreate(event => {
    const postId = event.params.postId;
    const post = event.data.val();
    const postAuthorId = post.authorId;
    const post_points = 3;
    console.log('Post created. ', postId);
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