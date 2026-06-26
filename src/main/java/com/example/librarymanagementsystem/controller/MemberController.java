package com.example.librarymanagementsystem.controller;


import com.example.librarymanagementsystem.dto.MemberDTO;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Management", description = "APIs for managing library members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "Register a new member")
    public ResponseEntity<Member> registerMember(@Valid @RequestBody MemberDTO memberDTO) {
        return new ResponseEntity<>(memberService.registerMember(memberDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all members")
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member")
    public ResponseEntity<Member> updateMember(@PathVariable Long id, @Valid @RequestBody MemberDTO memberDTO) {
        return ResponseEntity.ok(memberService.updateMember(id, memberDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete member")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search members by last name")
    public ResponseEntity<List<Member>> searchMembers(@RequestParam String lastName) {
        return ResponseEntity.ok(memberService.searchMembersByName(lastName));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update member status")
    public ResponseEntity<Void> updateMemberStatus(@PathVariable Long id, @RequestParam String status) {
        memberService.updateMemberStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
