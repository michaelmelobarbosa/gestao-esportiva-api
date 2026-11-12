package br.gov.quixada.sports.category;

import br.gov.quixada.sports.category.exception.DuplicateCategoryException;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final CompetitionRepository competitionRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Category> findAll(String name, CategoryStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Category findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    @Transactional
    public Category save(Category category, Long competitionId) {
        category.validate();
        Competition competition = findCompetition(competitionId);
        validateEditableCompetition(competition);
        validateDuplicateRange(competitionId, category.getMinAge(), category.getMaxAge(), null);

        category.activate();
        category.setCompetition(competition);
        return repository.save(category);
    }

    @Transactional
    public Category activate(Long id) {
        Category category = findByIdOrThrowNotFound(id);
        validateEditableCompetition(category.getCompetition());
        validateDuplicateRange(competitionId(category), category.getMinAge(),
                category.getMaxAge(), category.getId());
        category.activate();
        return category;
    }

    @Transactional
    public Category deactivate(Long id) {
        Category category = findByIdOrThrowNotFound(id);
        if (teamRegistrationRepository.existsByCategoryIdAndStatusIn(id, RegistrationStatus.OCCUPY_SLOT)) {
            throw new ActiveReferenceException("Category has active registrations and cannot be deactivated");
        }
        category.deactivate();
        return category;
    }

    /**
     * Updates mutable data only (name and age range). Competition is immutable after creation.
     */
    @Transactional
    public Category update(Long id, Category categoryToUpdate) {
        Category existingCategory = findByIdOrThrowNotFound(id);
        validateEditableCompetition(existingCategory.getCompetition());
        validateDuplicateRange(competitionId(existingCategory),
                categoryToUpdate.getMinAge(), categoryToUpdate.getMaxAge(), id);
        existingCategory.updateDetails(
                categoryToUpdate.getName(),
                categoryToUpdate.getMinAge(),
                categoryToUpdate.getMaxAge()
        );
        return existingCategory;
    }

    private void validateDuplicateRange(Long competitionId, Integer minAge, Integer maxAge, Long ignoredId) {
        if (competitionId == null) {
            return;
        }
        boolean duplicate = ignoredId == null
                ? repository.existsByCompetitionIdAndMinAgeAndMaxAgeAndStatus(
                        competitionId, minAge, maxAge, CategoryStatus.ACTIVE)
                : repository.existsByCompetitionIdAndMinAgeAndMaxAgeAndStatusAndIdNot(
                        competitionId, minAge, maxAge, CategoryStatus.ACTIVE, ignoredId);
        if (duplicate) {
            throw new DuplicateCategoryException("An active category with this age range already exists in this competition");
        }
    }

    private void validateEditableCompetition(Competition competition) {
        if (competition == null) {
            return;
        }
        CompetitionStatus status = competition.calculateStatus(LocalDate.now(clock));
        if (status == CompetitionStatus.CANCELED || status == CompetitionStatus.COMPLETED) {
            throw new CompetitionNotEditableException(
                    "A canceled or completed competition does not allow category changes");
        }
    }

    private Long competitionId(Category category) {
        return category.getCompetition() != null ? category.getCompetition().getId() : null;
    }

    private Competition findCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new CompetitionNotFoundException("Competition not found"));
    }
}
