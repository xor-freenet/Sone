/* Sone JavaScript functions. */

function isOnline() {
	return $("#sone").hasClass("online");
}

function registerInputTextareaSwap(inputSelector, defaultText, inputFieldName, optional, dontUseTextarea) {
	$(inputSelector).each(function() {
		textarea = $(dontUseTextarea ? "<input type=\"text\" name=\"" + inputFieldName + "\">" : "<textarea name=\"" + inputFieldName + "\"></textarea>").blur(function() {
			if ($(this).val() == "") {
				$(this).hide();
				inputField = $(this).data("inputField");
				inputField.show().removeAttr("disabled").addClass("default");
				(function(inputField) {
					getTranslation(defaultText, function(translation) {
						inputField.val(translation);
					});
				})(inputField);
			}
		}).hide().data("inputField", $(this)).val($(this).val());
		$(this).after(textarea);
		(function(inputField, textarea) {
			inputField.focus(function() {
				$(this).hide().attr("disabled", "disabled");
				textarea.show().focus();
			});
			if (inputField.val() == "") {
				inputField.addClass("default");
				(function(inputField) {
					getTranslation(defaultText, function(translation) {
						inputField.val(translation);
					});
				})(inputField);
			} else {
				inputField.hide().attr("disabled", "disabled");
				textarea.show();
			}
			$(inputField.get(0).form).submit(function() {
				if (!optional && (textarea.val() == "")) {
					return false;
				}
			});
		})($(this), textarea);
	});
}

/* hide all the “create reply” forms until a link is clicked. */
function addCommentLinks() {
	if (!isOnline()) {
		return;
	}
	$("#sone .post").each(function() {
		postId = $(this).attr("id");
		commentElement = (function(postId) {
			var commentElement = $("<div><span>Comment</span></div>").addClass("show-reply-form").click(function() {
				replyElement = $("#sone .post#" + postId + " .create-reply");
				replyElement.removeClass("hidden");
				replyElement.removeClass("light");
				(function(replyElement) {
					replyElement.find("input.reply-input").blur(function() {
						if ($(this).hasClass("default")) {
							replyElement.addClass("light");
						}
					}).focus(function() {
						replyElement.removeClass("light");
					});
				})(replyElement);
				replyElement.find("input.reply-input").focus();
			});
			return commentElement;
		})(postId);
		$(this).find(".create-reply").addClass("hidden");
		$(this).find(".status-line .time").each(function() {
			$(this).after(commentElement.clone(true));
		});
	});
}

/**
 * Retrieves the translation for the given key and calls the callback function.
 * The callback function takes a single parameter, the translated string.
 *
 * @param key
 *            The key of the translation string
 * @param callback
 *            The callback function
 */
function getTranslation(key, callback) {
	$.getJSON("ajax/getTranslation.ajax", {"key": key}, function(data, textStatus) {
		callback(data.value);
	});
}

/**
 * Fires off an AJAX request to retrieve the current status of a Sone.
 *
 * @param soneId
 *            The ID of the Sone
 */
function getSoneStatus(soneId) {
	$.getJSON("ajax/getSoneStatus.ajax", {"sone": soneId}, function(data, textStatus) {
		updateSoneStatus(soneId, data.name, data.status, data.modified, data.lastUpdated);
		/* seconds! */
		updateInterval = 60;
		if (data.modified || (data.status == "downloading") || (data.status == "inserting")) {
			updateInterval = 5;
		}
		setTimeout(function() {
			getSoneStatus(soneId);
		}, updateInterval * 1000);
	});
}

/**
 * Updates the status of the given Sone.
 *
 * @param soneId
 *            The ID of the Sone to update
 * @param status
 *            The status of the Sone (“idle”, “unknown”, “inserting”,
 *            “downloading”)
 * @param modified
 *            Whether the Sone is modified
 * @param lastUpdated
 *            The date and time of the last update (formatted for display)
 */
function updateSoneStatus(soneId, name, status, modified, lastUpdated) {
	$("#sone .sone." + soneId).
		toggleClass("unknown", status == "unknown").
		toggleClass("idle", status == "idle").
		toggleClass("inserting", status == "inserting").
		toggleClass("downloading", status == "downloading").
		toggleClass("modified", modified);
	$("#sone .sone." + soneId + " .last-update span.time").text(lastUpdated);
	$("#sone .sone." + soneId + " .profile-link a").text(name);
}

var watchedSones = {};

/**
 * Watches this Sone for updates to its status.
 *
 * @param soneId
 *            The ID of the Sone to watch
 */
function watchSone(soneId) {
	if (watchedSones[soneId]) {
		return;
	}
	watchedSones[soneId] = true;
	(function(soneId) {
		setTimeout(function() {
			getSoneStatus(soneId);
		}, 5000);
	})(soneId);
}

/**
 * Enhances a “delete” button so that the confirmation is done on the same page.
 *
 * @param buttonId
 *            The selector of the button
 * @param translationKey
 *            The translation key of the text to show on the button
 * @param deleteCallback
 *            The callback that actually deletes something
 */
function enhanceDeleteButton(buttonId, translationKey, deleteCallback) {
	button = $(buttonId);
	(function(button) {
		getTranslation(translationKey, function(translation) {
			newButton = $("<button></button>").addClass("confirm").hide().text(translation).click(function() {
				$(this).fadeOut("slow");
				deleteCallback();
				return false;
			}).insertAfter(button);
			(function(button, newButton) {
				button.click(function() {
					button.fadeOut("slow", function() {
						newButton.fadeIn("slow");
						$(document).one("click", function() {
							if (this != newButton.get(0)) {
								newButton.fadeOut(function() {
									button.fadeIn();
								});
							}
						});
					});
					return false;
				});
			})(button, newButton);
		});
	})(button);
}

/**
 * Enhances a post’s “delete” button.
 *
 * @param buttonId
 *            The selector of the button
 * @param postId
 *            The ID of the post to delete
 */
function enhanceDeletePostButton(buttonId, postId) {
	enhanceDeleteButton(buttonId, "WebInterface.Confirmation.DeletePostButton", function() {
		$.getJSON("ajax/deletePost.ajax", { "post": postId, "formPassword": $("#sone #formPassword").text() }, function(data, textStatus) {
			if (data.success) {
				$("#sone .post#" + postId).slideUp();
			} else if (data.error == "invalid-post-id") {
				alert("Invalid post ID given!");
			} else if (data.error == "auth-required") {
				alert("You need to be logged in.");
			} else if (data.error == "not-authorized") {
				alert("You are not allowed to delete this post.");
			}
		});
	});
}

/**
 * Enhances a reply’s “delete” button.
 *
 * @param buttonId
 *            The selector of the button
 * @param replyId
 *            The ID of the reply to delete
 */
function enhanceDeleteReplyButton(buttonId, replyId) {
	enhanceDeleteButton(buttonId, "WebInterface.Confirmation.DeleteReplyButton", function() {
		$.getJSON("ajax/deleteReply.ajax", { "reply": replyId, "formPassword": $("#sone #formPassword").text() }, function(data, textStatus) {
			if (data.success) {
				$("#sone .reply#" + replyId).slideUp();
			} else if (data.error == "invalid-reply-id") {
				alert("Invalid reply ID given!");
			} else if (data.error == "auth-required") {
				alert("You need to be logged in.");
			} else if (data.error == "not-authorized") {
				alert("You are not allowed to delete this reply.");
			}
		});
	});
}

function getFormPassword() {
	return $("#sone #formPassword").text();
}

function getSoneElement(element) {
	return $(element).parents(".sone");
}

/**
 * Returns the ID of the Sone that this element belongs to.
 *
 * @param element
 *            The element to locate the matching Sone ID for
 * @returns The ID of the Sone, or undefined
 */
function getSoneId(element) {
	return getSoneElement(element).find(".id").text();
}

function getPostElement(element) {
	return $(element).parents(".post");
}

function getPostId(element) {
	return getPostElement(element).attr("id");
}

function getReplyElement(element) {
	return $(element).parents(".reply");
}

function getReplyId(element) {
	return getReplyElement(element).attr("id");
}

function likePost(postId) {
	$.getJSON("ajax/like.ajax", { "type": "post", "post" : postId, "formPassword": getFormPassword() }, function() {
		$("#sone .post#" + postId + " > .status-line .like").addClass("hidden");
		$("#sone .post#" + postId + " > .status-line .unlike").removeClass("hidden");
		updatePostLikes(postId);
	});
}

function unlikePost(postId) {
	$.getJSON("ajax/unlike.ajax", { "type": "post", "post" : postId, "formPassword": getFormPassword() }, function() {
		$("#sone .post#" + postId + " > .status-line .unlike").addClass("hidden");
		$("#sone .post#" + postId + " > .status-line .like").removeClass("hidden");
		updatePostLikes(postId);
	});
}

function updatePostLikes(postId) {
	$.getJSON("ajax/getLikes.ajax", { "type": "post", "post": postId }, function(data, textStatus) {
		if (data.success) {
			$("#sone .post#" + postId + " > .status-line .likes").toggleClass("hidden", data.likes == 0)
			$("#sone .post#" + postId + " > .status-line .likes span.like-count").text(data.likes);
		}
	});
}

function likeReply(replyId) {
	$.getJSON("ajax/like.ajax", { "type": "reply", "reply" : replyId, "formPassword": getFormPassword() }, function() {
		$("#sone .reply#" + replyId + " .status-line .like").addClass("hidden");
		$("#sone .reply#" + replyId + " .status-line .unlike").removeClass("hidden");
		updateReplyLikes(replyId);
	});
}

function unlikeReply(replyId) {
	$.getJSON("ajax/unlike.ajax", { "type": "reply", "reply" : replyId, "formPassword": getFormPassword() }, function() {
		$("#sone .reply#" + replyId + " .status-line .unlike").addClass("hidden");
		$("#sone .reply#" + replyId + " .status-line .like").removeClass("hidden");
		updateReplyLikes(replyId);
	});
}

function updateReplyLikes(replyId) {
	$.getJSON("ajax/getLikes.ajax", { "type": "reply", "reply": replyId }, function(data, textStatus) {
		if (data.success) {
			$("#sone .reply#" + replyId + " .status-line .likes").toggleClass("hidden", data.likes == 0)
			$("#sone .reply#" + replyId + " .status-line .likes span.like-count").text(data.likes);
		}
	});
}
