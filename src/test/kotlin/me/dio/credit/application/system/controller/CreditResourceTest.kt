package me.dio.credit.application.system.controller

import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Address
// import me.dio.credit.application.system.service.CreditService
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.repository.CreditRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.MvcResult
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Random
import java.util.UUID
import org.assertj.core.api.Assertions

import io.mockk.impl.annotations.MockK

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
  @Autowired private lateinit var creditRepository: CreditRepository
  @Autowired private lateinit var mockMvc: MockMvc
  @Autowired private lateinit var objectMapper: ObjectMapper

  @Autowired private lateinit var customerService: CustomerService
  @Autowired private lateinit var creditService: CreditService

  companion object {
    const val URL: String = "/api/credits"
    const val URL_CUSTOMER: String = "/api/customers"

    private fun buildCredit(
      creditValue: BigDecimal = BigDecimal.valueOf(2000.0),
      dayFirstInstallment: LocalDate = LocalDate.now().plusMonths(2L),
      numberOfInstallments: Int = 10,
      customer: Customer = buildCustomer()
    ): Credit = Credit(
      creditValue = creditValue,
      dayFirstInstallment = dayFirstInstallment,
      numberOfInstallments = numberOfInstallments,
      customer = customer
    )

    private fun buildCustomer(
      firstName: String = "Bruno",
      lastName: String = "Leo",
      cpf: String = "57861728218",
      email: String = "bruno@email.com",
      password: String = "123456",
      zipCode: String = "81270000",
      street: String = "Av do Batel, 1200",
      income: BigDecimal = BigDecimal.valueOf(10000.0),
      id: Long = 1L
    ) = Customer(
      firstName = firstName,
      lastName = lastName,
      cpf = cpf,
      email = email,
      password = password,
      address = Address(
        zipCode = zipCode,
        street = street,
      ),
      income = income,
      id = id
    )
  }

  /*
  @BeforeEach
  fun setUp() {
    creditService = CreditService(creditRepository, customerService)
  }
  */

  // @BeforeEach fun setup() = creditRepository.deleteAll()
  // @AfterEach fun tearDown() = creditRepository.deleteAll()

  @Test
  fun `should create a customer first and return 201 status`(){
    //given
    // customer
    val customerDto: CustomerDto = builderCustomerDto()
    val valueAsStringCustomer: String = objectMapper.writeValueAsString(customerDto)

    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL_CUSTOMER)
        .contentType(MediaType.APPLICATION_JSON)
        .content(valueAsStringCustomer)
    )
      .andExpect(MockMvcResultMatchers.status().isCreated)
      .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Bruno"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Leonardo"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.cpf").value("36994871649"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("brunoleonardo@email.com"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.income").value("5000.0"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.zipCode").value("81270002"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.street").value("Av do Batel, 100"))
      // .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should create a credit and return 201 status`() {
    //given
    val creditDto: CreditDto = builderCreditDto()
    val valueAsString: String = objectMapper.writeValueAsString(creditDto)

    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(valueAsString)
    )
      .andExpect(MockMvcResultMatchers.status().isCreated)
      .andExpect(MockMvcResultMatchers.header().string("content-type", "text/plain;charset=UTF-8"))
      // .andExpect(MockMvcResultMatchers.content().string(contains("brunoleonardo@email.com")))
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not create a credit because customer doesnt exists and return 400 status`() {
    //given

    // credit
    val creditDto: CreditDto = builderCreditDto()
    val valueAsString: String = objectMapper.writeValueAsString(creditDto)

    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(valueAsString)
    )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
      .andExpect(
        MockMvcResultMatchers.jsonPath("$.exception")
           .value("class me.dio.credit.application.system.exception.BusinessException")
      )
      .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should find all credit by customer id and return 200 status`() {
    //given
    val creditDto: Credit = creditRepository.save(builderCreditDto().toEntity())

    //when
    //then
    Assertions.assertThat(creditDto.customer?.id).isEqualTo(1)

    mockMvc.perform(
      MockMvcRequestBuilders
        .get("$URL?customerId=${creditDto.customer?.id}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].creditCode").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].creditValue").value("2000.0"))
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].numberOfInstallments").value("10"))
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should find by creditCode and return 200 status`() {
    //given
    val customerId: Long = 1L

    val credit: Credit = buildCredit(customer = Customer(id = customerId))
    val customer: Customer = buildCustomer()
    customerService.save(customer)
    creditService.save(credit)

    val creditServiceData: List<Credit> = creditService.findAllByCustomer(customerId)

    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders
        .get("$URL/${creditServiceData[0].creditCode}?customerId=${customerId}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value("2000.0"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value("10"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("bruno@email.com"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value("10000.0"))
      .andDo(MockMvcResultHandlers.print())
  }

  private fun builderCustomerDto(
    firstName: String = "Bruno",
    lastName: String = "Leonardo",
    cpf: String = "36994871649",
    email: String = "brunoleonardo@email.com",
    income: BigDecimal = BigDecimal.valueOf(5000.0),
    password: String = "123456",
    zipCode: String = "81270002",
    street: String = "Av do Batel, 100",
  ) = CustomerDto(
    firstName = firstName,
    lastName = lastName,
    cpf = cpf,
    email = email,
    income = income,
    password = password,
    zipCode = zipCode,
    street = street
  )

  private fun builderCreditDto(
    creditValue: BigDecimal = BigDecimal.valueOf(2000.0),
    dayFirstOfInstallment: LocalDate = LocalDate.now().plusMonths(2L),
    numberOfInstallments: Int = 10,
    customerId: Long = 1L

  ) = CreditDto(
    creditValue = creditValue,
    dayFirstOfInstallment = dayFirstOfInstallment,
    numberOfInstallments = numberOfInstallments,
    customerId = customerId
  )
}
