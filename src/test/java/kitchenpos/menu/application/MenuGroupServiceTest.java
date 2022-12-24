package kitchenpos.menu.application;

import kitchenpos.menu.domain.fixture.MenuGroupFixture;
import kitchenpos.menu.dto.MenuGroupCreateRequest;
import kitchenpos.menu.repository.MenuGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static kitchenpos.common.fixture.NameFixture.MENU_GROUP_A_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("MenuGroupServiceTest")
@ExtendWith(MockitoExtension.class)
class MenuGroupServiceTest {

    @InjectMocks
    private MenuGroupService menuGroupService;

    @Mock
    private MenuGroupRepository menuGroupRepository;

    @DisplayName("메뉴 그룹 생성")
    @Test
    void create() {

        given(menuGroupRepository.save(any())).willReturn(MenuGroupFixture.menuGroupA());

        assertThat(menuGroupService.create(new MenuGroupCreateRequest(MenuGroupFixture.menuGroupA().getName())).getName())
                .isEqualTo(MENU_GROUP_A_NAME);
    }

    @DisplayName("메뉴 그룹 목록을 조회한다.")
    @Test
    void name() {

        given(menuGroupRepository.findAll()).willReturn(Collections.singletonList(MenuGroupFixture.menuGroupA()));

        assertThat(menuGroupService.list()).hasSize(1);
    }
}
