package com.github.alexandergillon.wegapi.game_action;

public class Message extends GameAction {
    public enum MessageType {
        DEFAULT,
        ERROR
    }

    private String message;
    private MessageType messageType;

    public Message(String message) {
        actionType = ActionType.DISPLAY_MESSAGE;
        this.message = message;
    }

    public String getMessage() {
        return message;
    };

    public MessageType getMessageType() {
        return messageType;
    }
}
