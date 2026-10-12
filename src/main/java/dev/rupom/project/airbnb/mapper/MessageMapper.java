package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.ConversationResponse;
import dev.rupom.project.airbnb.dto.MessageResponse;
import dev.rupom.project.airbnb.entity.Conversation;
import dev.rupom.project.airbnb.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "sender.name", target = "senderName")
    MessageResponse toMessageResponse(Message message);

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "guest.id", target = "guestId")
    @Mapping(source = "guest.name", target = "guestName")
    @Mapping(source = "host.id", target = "hostId")
    @Mapping(source = "host.name", target = "hostName")
    ConversationResponse toConversationResponse(Conversation conversation);
}
