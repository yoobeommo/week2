package com.example.week2.service;

import com.example.week2.dto.BlogRequestDto;
import com.example.week2.dto.BlogResponseDto;
import com.example.week2.dto.ResponseDto;
import com.example.week2.entity.Blog;
import com.example.week2.entity.Timestamped;
import com.example.week2.entity.User;
import com.example.week2.entity.UserRoleEnum;
import com.example.week2.jwt.JwtUtil;
import com.example.week2.repository.BlogRepository;
import com.example.week2.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


    @Transactional
    public BlogResponseDto createBlog(BlogRequestDto requestDto, HttpServletRequest request) {
        // Request에서 Token 가져오기
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        // 토큰이 있는 경우에만 관심상품 추가 가능
        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                // 토큰에서 사용자 정보 가져오기
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }

            // 토큰에서 가져온 사용자 정보를 사용하여 DB 조회
            User user = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new IllegalArgumentException("사용자가 존재하지 않습니다.")
            );

            // 요청받은 DTO 로 DB에 저장할 객체 만들기
            Blog blog = blogRepository.saveAndFlush(new Blog(requestDto, user.getUsername(), user.getId()));

            return new BlogResponseDto(blog);
        } else {
            return null;
        }
    }

    public List<BlogResponseDto> getBlogList() {
        // 테이블에 저장되어있는 모든 게시글 목록을 조회
        return blogRepository.findAll().stream().sorted(Comparator.comparing(Timestamped::getCreatedAt).reversed())
                .map(BlogResponseDto::new).collect(Collectors.toList());
    }

    public BlogResponseDto getBlog(Long id) {
        // 조회하기 위해 받아온 Blog 의 id를 사용해서 해당 Blog 인스턴스가 테이블에 존재하는지 확인하고 가져옵니다.
        Blog blog = checkBlog(id);
        return new BlogResponseDto(blog);
    }

    @Transactional
    public BlogResponseDto updateBlog(Long id, BlogRequestDto requestDto, HttpServletRequest request) {
        // Request에서 Token 가져오기
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        // 토큰이 있는 경우에만 관심상품 최저가 업데이트 가능
        if (token != null) {
            // Token 검증
            if (jwtUtil.validateToken(token)) {
                // 토큰에서 사용자 정보 가져오기
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }

            // 토큰에서 가져온 사용자 정보를 사용하여 DB 조회
            User user = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new IllegalArgumentException("사용자가 존재하지 않습니다.")
            );

            Blog blog = blogRepository.findByIdAndUserId(id, user.getId()).orElseThrow(
                    () -> new NullPointerException("해당 게시글은 존재하지 않습니다.")
            );

            blog.update(requestDto);
            return new BlogResponseDto(blog);
        }
         throw new IllegalArgumentException("토큰이 존재하지 않습니다.");
    }
    // Delete DB Function
    public ResponseDto deleteBlog(Long id, HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Claims claims;

        // 토큰이 있는 경우에만 관심상품 추가 가능
        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                // 토큰에서 사용자 정보 가져오기
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }

            User user = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new IllegalArgumentException("사용자가 존재하지 않습니다.")
            );

            UserRoleEnum userRoleEnum = user.getRole();
            Blog blog;

            if (userRoleEnum == UserRoleEnum.USER) {
                blog = blogRepository.findById(id).orElseThrow(
                        () -> new NullPointerException("해당 게시글은 존재하지 않습니다.")
                );

                if (blog.getUserId().equals(user.getId())) {
                    blogRepository.deleteById(id);
                } else {
                    throw new IllegalArgumentException("게시물 삭제 권한이 없습니다.");
                }
            } else {                                                                                          // User 권한이 Admin일 경우 모든 내용 수정 가능
                blog = blogRepository.findById(id).orElseThrow(
                        () -> new NullPointerException("해당 게시글은 존재하지 않습니다.")
                );
                blogRepository.deleteById(id);                                                                // 삭제 처리
            }
//            String loginUserName = user.getUsername();
//            if (!blog.getUsername().equals(loginUserName)) {
//                throw new IllegalArgumentException("회원님의 게시글이 아닙니다!");
//            }
            return new ResponseDto("success", HttpStatus.OK.value());
        } else {
            throw new IllegalArgumentException("토큰이 존재하지 않습니다.");
        }
    }

//    public String deleteBlog(Long id, String password) {
//        // 수정하기 위해 받아온 Blog 의 id를 사용하여 해당 Blog 인스턴스가 존재하는지 확인하고 가져옵니다.
//        Blog blog = checkBlog(id);
//        if (blog.getPassword().equals(password)) {
//            blogRepository.delete(blog);
//            return "게시글 삭제에 성공했습니다.";
//        } else {
//            // 원래 NullPointerException을 사용했지만 IllegalArgumentException 이 상황에 더 알맞아서 변경.
//            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
//        }
//    }

    public BlogResponseDto getBlogByTitle(String title) {
        Blog blog = blogRepository.findByTitle(title).orElseThrow(
                () -> new IllegalArgumentException("해당하는 제목의 게시글이 없습니다.")
                // 원래 NullPointerException을 사용했지만 IllegalArgumentException 이 상황에 더 알맞아서 변경.
        );
        return new BlogResponseDto(blog);
    }

    private Blog checkBlog(Long id) {
        return blogRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("일치하는 ID가 없습니다.")
                // 원래 NullPointerException을 사용했지만 IllegalArgumentException 이 상황에 더 알맞아서 변경.
        );
    }
}