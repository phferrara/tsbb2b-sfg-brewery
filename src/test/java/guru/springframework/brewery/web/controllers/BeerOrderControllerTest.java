package guru.springframework.brewery.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.brewery.services.BeerOrderService;
import guru.springframework.brewery.web.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerOrderController.class)
class BeerOrderControllerTest {
    @MockBean
    private BeerOrderService beerOrderService;
    @Autowired
    private MockMvc mockMvc;

    private BeerOrderDto beerOrder;
    private BeerOrderPagedList beerOrders;

    @BeforeEach
    void setUp() {
        BeerDto validBeer = BeerDto.builder().id(UUID.randomUUID())
                .version(1)
                .beerName("Beer1")
                .beerStyle(BeerStyleEnum.PALE_ALE)
                .price(new BigDecimal("12.99"))
                .quantityOnHand(4)
                .upc(123456789012L)
                .createdDate(OffsetDateTime.now())
                .lastModifiedDate(OffsetDateTime.now())
                .build();

        beerOrder = BeerOrderDto.builder()
                .id(UUID.randomUUID())
                .customerRef("1234")
                .customerId(UUID.randomUUID())
                .beerOrderLines(List.of(BeerOrderLineDto
                        .builder()
                        .beerId(validBeer.getId())
                        .build()))
                .build();

        beerOrders = new BeerOrderPagedList(List.of(beerOrder), PageRequest.of(1, 1), 1L);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(beerOrderService);
    }

    @Test
    void listOrders() throws Exception {
        UUID customerId = UUID.randomUUID();
        Integer pageNumber = 1;
        Integer pageSize = 1;
        given(beerOrderService.listOrders(customerId, PageRequest.of(pageNumber, pageSize)))
                .willReturn(beerOrders);

        mockMvc.perform(get("/api/v1/customers/{customerId}/orders", customerId)
                        .param("pageNumber", String.valueOf(pageNumber))
                        .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
    }

    @Test
    void placeOrder() throws Exception {
        UUID customerId = beerOrder.getCustomerId();
        given(beerOrderService.placeOrder(eq(customerId), argThat(new BeerOrderDtoMatcher(beerOrder))))
                .willReturn(beerOrder);

        mockMvc.perform(post("/api/v1/customers/{customerId}/orders", customerId)
                .content(asJsonString(beerOrder))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.customerId", is(customerId.toString())));
    }

    class BeerOrderDtoMatcher implements ArgumentMatcher<BeerOrderDto> {

        private BeerOrderDto beerOrderDto;

        BeerOrderDtoMatcher(BeerOrderDto beerOrderDto) {
            this.beerOrderDto = beerOrderDto;
        }

        @Override
        public boolean matches(BeerOrderDto that) {
            return this.beerOrderDto.getCustomerRef().equals(that.getCustomerRef());
        }
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getOrder() throws Exception {
        UUID customerId = beerOrder.getCustomerId();
        UUID orderId = UUID.randomUUID();
        given(beerOrderService.getOrderById(customerId, orderId))
                .willReturn(beerOrder);

        mockMvc.perform(get("/api/v1/customers/{customerId}/orders/{orderId}", customerId, orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.customerId", is(customerId.toString())));
    }

    @Test
    void pickupOrder() {
    }
}