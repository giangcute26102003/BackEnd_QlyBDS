//package com.example.qlyBDS.controllers;
//
//import com.example.qlyBDS.dtos.PropertyDTO;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("api/v1/properties")
////@Validated
//public class PropertyController {
//
//    @GetMapping("/property")
//    public ResponseEntity<PropertyDTO> getALLProperties() {
//        return ResponseEntity.ok("Hello World");
//    }
//    @GetMapping("/a")
//    public ResponseEntity<String> getProperties(
//            @RequestParam("page") int page,
//            @RequestParam("limit")  int limit
//    ) {
//
//        return ResponseEntity.ok("this is the properties page"+page+"/"+limit);
//    }
//    @GetMapping("/b")
//    public ResponseEntity<?> getAProperties(
//        @Valid  @RequestBody PropertyDTO propertyDTO, BindingResult bindingResult
//    ) {
//        if(bindingResult.hasErrors()) {
////            bindingResult.getFieldError().getDefaultMessage();
//            return ResponseEntity.badRequest().body(bindingResult.getFieldError().getDefaultMessage());
//
//        }
//        return ResponseEntity.ok( "Hello " + propertyDTO);
//    }
//    @PostMapping("")
//    public ResponseEntity<String> insertProperties() {
//        return ResponseEntity.ok("insert new properties");
//    }
//    @PutMapping("/{id}")
//    public ResponseEntity<String> updateProperties(@PathVariable String id) {
//        return ResponseEntity.ok("This is the updated properties with id " + id);
//    }
//    @DeleteMapping("/{id}")
//    public ResponseEntity<String> deleteProperties(@PathVariable String id) {
//        return ResponseEntity.ok("This is the delete properties with id " + id);
//    }
//}
