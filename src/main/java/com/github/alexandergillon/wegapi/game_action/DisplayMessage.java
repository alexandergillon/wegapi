package com.github.alexandergillon.wegapi.game_action;

public class DisplayMessage extends GameAction {
    public enum MessageType {
        DEFAULT,
        ERROR
    }

    private String message;
    private MessageType messageType;

    public DisplayMessage(String message, MessageType messageType) {
        actionType = ActionType.DISPLAY_MESSAGE;
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    };

    public MessageType getMessageType() {
        return messageType;
    }
}
