package kitchenpos.product.domain;

import kitchenpos.product.domain.fixture.ProductFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static kitchenpos.common.fixture.NameFixture.nameProductA;
import static kitchenpos.common.fixture.PriceFixture.priceProductA;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품")
class ProductTest {

    @DisplayName("상품 생성")
    @Test
    void create() {
        assertThatNoException().isThrownBy(ProductFixture::productA);
    }

    @DisplayName("이름을 필수로 갖는다.")
    @Test
    void name() {
        assertThatThrownBy(() -> new Product(1L, null, priceProductA()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("가격을 필수로 갖는다.")
    @Test
    void price() {
        assertThatThrownBy(() -> new Product(1L, nameProductA(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
