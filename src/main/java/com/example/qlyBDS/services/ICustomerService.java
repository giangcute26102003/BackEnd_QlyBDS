//package com.example.qlyBDS.services;
//
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//public interface ICustomerService {
//    CustomerResponse createCustomer(CreateCustomerRequest request);
//    CustomerResponse updateCustomer(Integer customerId, UpdateCustomerRequest request);
//    void deleteCustomer(Integer customerId);
//    CustomerResponse getCustomerById(Integer customerId);
//    Page<CustomerResponse> getAllCustomers(Pageable pageable);
//    Page<CustomerResponse> searchCustomers(String keyword, Pageable pageable);
//}