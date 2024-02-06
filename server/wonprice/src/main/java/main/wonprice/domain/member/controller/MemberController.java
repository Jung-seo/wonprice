package main.wonprice.domain.member.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.wonprice.domain.member.dto.*;
import main.wonprice.domain.member.entity.Member;
import main.wonprice.domain.member.mapper.MemberMapper;
import main.wonprice.domain.member.service.MemberService;
import main.wonprice.domain.picture.entity.MemberPicture;
import main.wonprice.domain.picture.entity.Picture;
import main.wonprice.domain.picture.service.PictureService;
import main.wonprice.domain.product.dto.ProductResponseDto;
import main.wonprice.domain.product.entity.Product;
import main.wonprice.domain.product.mapper.ProductMapper;
import main.wonprice.domain.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/members")
@AllArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final ProductService productService;
    private final MemberMapper mapper;
    private final ProductMapper productMapper;

    private final PictureService pictureService;

//    회원 가입
    @PostMapping
    public ResponseEntity postMember(@RequestBody @Valid MemberPostDto postDto) {

        Member member = mapper.postDtoToMember(postDto);
        memberService.joinMember(member);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

//    프로필 정보 조회
    @GetMapping("/{member-id}")
    public ResponseEntity getProfile(@PathVariable("member-id") Long memberId) {

        Member member = memberService.findMember(memberId);
        MemberResponseDto response = mapper.memberToResponseDto(member, productService.getMembersProductCount(member));

        if (response.getPicture() == null) {
            MemberPicture picture = new MemberPicture();
            picture.setPath("https://wonprice-test1.s3.ap-northeast-2.amazonaws.com/default_profile.png");
            response.setPicture(picture);
        }

        return new ResponseEntity(response, HttpStatus.OK);
    }

    // 회원 게시물 목록 조회
    @GetMapping("/{member-id}/products")
    public ResponseEntity findMembersProduct(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @PathVariable("member-id") Long memberId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Member member = memberService.findMember(memberId);

        Page<Product> products = productService.findMembersProduct(pageable, member);
        Page<ProductResponseDto> response = products.map(productMapper::fromEntity);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    판매 완료 목록 조회
    @GetMapping("/{member-id}/sell")
    public ResponseEntity findMembersSell(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @PathVariable("member-id") Long memberId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Member member = memberService.findMember(memberId);
        Page<Product> products = productService.findMemberSold(pageable, member);
        Page<ProductResponseDto> response = products.map(productMapper::fromEntity);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    구매 완료 목록 조회
    @GetMapping("/{member-id}/purchase")
    public ResponseEntity findMembersPurchase(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @PathVariable("member-id") Long memberId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Product> products = productService.findMemberBought(pageable, memberId);
        Page<ProductResponseDto> response = products.map(productMapper::fromEntity);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{member-id}/bids")
    public ResponseEntity findMembersBids(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @PathVariable("member-id") Long memberId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Product> products = productService.findMembersBidProducts(pageable, memberId);
        Page<ProductResponseDto> response = products.map(productMapper::fromEntity);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    회원 목록 조회
    @GetMapping("/all")
    public ResponseEntity getMembers(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Member> members = memberService.findMembers(pageable);
        Page<MemberResponseDto> response = members.map(mapper::memberToResponseDto);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    회원 정보 수정
    @PatchMapping("/{member-id}")
    public ResponseEntity patchMember(@PathVariable("member-id") Long memberId,
                                      @RequestBody @Valid MemberPatchDto patchDto) {

        Member member = mapper.patchDtoToMember(patchDto);
        member.setMemberId(memberId);
        Member patchedMember = memberService.updateMember(member);

        MemberResponseDto response = mapper.memberToResponseDto(patchedMember);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    회원 탈퇴
    @DeleteMapping("/{member-id}")
    public ResponseEntity deleteMember(@PathVariable("member-id") Long memberId) {
        memberService.deleteMember(memberId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

//    비밀번호 인증
    @PostMapping("/auth/password")
    public ResponseEntity checkPassword(@RequestBody AuthPasswordDto passwordDto) {

        memberService.validatePassword(passwordDto.getPassword());

        return new ResponseEntity<>(HttpStatus.OK);
    }

//    중복 이름 검증
    @PostMapping("/auth/name")
    public ResponseEntity checkName(@RequestBody Map<String, String> name) {

        String inputName = name.get("name");
        memberService.checkExistName(inputName);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{member-id}/image")
    public ResponseEntity createImage(@PathVariable(name = "member-id") Long memberId, @RequestBody Map<String, String> imageUrl) {

        Member findMember = memberService.findMember(memberId);

        if (!imageUrl.isEmpty()) {
            String path = imageUrl.get("path");
            log.info("path : " + path);
            pictureService.createPicture(path, findMember);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
