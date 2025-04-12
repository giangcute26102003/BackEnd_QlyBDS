//package com.example.qlyBDS.services;
//
//import com.realestate.dto.request.CreateInteractionRequest;
//import com.realestate.dto.request.UpdateInteractionRequest;
//import com.realestate.dto.response.InteractionResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import java.time.LocalDate;
//
//public interface IInteractionService {
//    InteractionResponse createInteraction(CreateInteractionRequest request);
//    InteractionResponse updateInteraction(Integer interactionId, UpdateInteractionRequest request);
//    void deleteInteraction(Integer interactionId);
//    InteractionResponse getInteractionById(Integer interactionId);
//    Page<InteractionResponse> getInteractionsByCustomer(Integer customerId, Pageable pageable);
//    Page<InteractionResponse> getInteractionsByProperty(Integer propertyId, Pageable pageable);
//    Page<InteractionResponse> getInteractionsByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);
//}