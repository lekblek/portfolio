package com.scalke.portfolio.backend.media.web.controller;

import com.scalke.portfolio.backend.media.application.usecase.DeleteMediaUseCase;
import com.scalke.portfolio.backend.media.application.usecase.GetMediaUseCase;
import com.scalke.portfolio.backend.media.application.usecase.ListMediaUseCase;
import com.scalke.portfolio.backend.media.application.usecase.MediaUpload;
import com.scalke.portfolio.backend.media.application.usecase.UpdateMediaAltTextUseCase;
import com.scalke.portfolio.backend.media.application.usecase.UploadMediaUseCase;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.web.dto.AdminMediaResponse;
import com.scalke.portfolio.backend.media.web.dto.UpdateMediaRequest;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Catalogue des médias pour l'administration (D-CT), derrière la session de l'administrateur.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/media")
public class AdminMediaController {

    private final UploadMediaUseCase uploadMediaUseCase;
    private final ListMediaUseCase listMediaUseCase;
    private final GetMediaUseCase getMediaUseCase;
    private final UpdateMediaAltTextUseCase updateMediaAltTextUseCase;
    private final DeleteMediaUseCase deleteMediaUseCase;

    /**
     * Envoi d'un fichier (partie {@code file}) et de son texte alternatif facultatif. Format reconnu par le contenu
     * (415 sinon), taille bornée par format puis requête bornée à 10 Mio (413 {@code MEDIA_TOO_LARGE}).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AdminMediaResponse> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(required = false) @Size(max = Media.MAX_ALT_TEXT_LENGTH) String altText) throws IOException {
        Media created;
        try (InputStream content = file.getInputStream()) {
            created = uploadMediaUseCase.execute(new MediaUpload(file.getOriginalFilename(), altText, content));
        }
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
            .body(AdminMediaResponse.from(created));
    }

    @GetMapping
    PageResponse<AdminMediaResponse> list(@PageableDefault(size = ApiPaging.ADMIN_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listMediaUseCase.execute(query).map(AdminMediaResponse::from));
    }

    @GetMapping("/{id}")
    AdminMediaResponse get(@PathVariable Long id) {
        return AdminMediaResponse.from(getMediaUseCase.execute(id));
    }

    @PatchMapping("/{id}")
    AdminMediaResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMediaRequest body) {
        return AdminMediaResponse.from(updateMediaAltTextUseCase.execute(id, body.altText()));
    }

    /**
     * Refusée tant qu'un contenu utilise le média : 409 {@code MEDIA_STILL_REFERENCED} (invariant 13).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        deleteMediaUseCase.execute(id);
    }
}
