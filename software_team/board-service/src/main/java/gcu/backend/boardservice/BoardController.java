package gcu.backend.boardservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@CrossOrigin("http://localhost:3000")
@RestController
@EnableJpaAuditing
public class BoardController {
    @Autowired
    private final BoardRepository boardRepository;
    public BoardController(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;

        String uploadDir = "uploads";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException ex) {
            throw new RuntimeException("이미지 업로드 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    @PostMapping("/api/board")
    public String create(@RequestParam("title") String title,
                         @RequestParam("author") String author,
                         @RequestParam("content") String content,
                         @RequestParam("category") String category,
                         @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Board board = new Board();
        board.setTitle(title);
        board.setAuthor(author);
        board.setContent(content);
        board.setCategory(category);

        if (image != null && !image.isEmpty()) {
            String fileName = StringUtils.cleanPath(image.getOriginalFilename());
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            board.setImage(filePath.toString());
        }

        boardRepository.save(board);
        return "board create ok";
    }


    @GetMapping("/api/board")
    public List<Board> getAllBoard() {
        return boardRepository.findAll();
    }

    @GetMapping("/api/board/{id}")
    public ResponseEntity<?> getBoardId(@PathVariable("id") Long id) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            // 게시글 데이터를 반환하는 코드
            return ResponseEntity.ok(board);
        } else {
            // 게시글이 없는 경우 NoPage로 리다이렉트하는 코드
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NoPage");
        }
    }

    @PostMapping("/api/board/recommendation")
    public String recommendBoard(@RequestBody RecommendationRequest request) {
        Long postId = request.getPostId();
        Optional<Board> optionalBoard = boardRepository.findById(postId);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            board.setRecommendation(board.getRecommendation() + 1);
            boardRepository.save(board);
            return "Board recommendation updated";
        } else {
            return "Board not found";
        }
    }

    @GetMapping("/api/board/ranked")
    public List<Board> getRankedBoard() {
        Sort sort = Sort.by(Sort.Direction.DESC, "recommendation");
        return boardRepository.findAll(sort);
    }

    @GetMapping("/api/board/image/{id}")
    public ResponseEntity<Resource> getBoardImage(@PathVariable("id") Long id) throws IOException {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            Path imagePath = Path.of(board.getImage());
            Resource imageResource = new UrlResource(imagePath.toUri());
            if (imageResource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // 이미지 타입에 맞게 설정
                        .body(imageResource);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/api/board/{id}")
    public ResponseEntity<String> updateBoard(
            @PathVariable("id") Long id,
            @RequestBody Board updatedBoard
    ) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            board.setTitle(updatedBoard.getTitle());
            board.setContent(updatedBoard.getContent());
            boardRepository.save(board);
            return ResponseEntity.ok("Board updated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/api/board/{id}")
    public String deleteBoard(@PathVariable("id") Long id) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();

            // Delete the associated image file if it exists
            if (board.getImage() != null) {
                Path imagePath = Path.of(board.getImage());
                try {
                    Files.deleteIfExists(imagePath);
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to delete the image file.", ex);
                }
            }

            boardRepository.delete(board);
            return "Board deleted successfully";
        } else {
            return "Board not found";
        }
    }


}

