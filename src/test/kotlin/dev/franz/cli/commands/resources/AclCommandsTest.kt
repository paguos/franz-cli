package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.Acl
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType
import dev.franz.cli.kafka.repository.AclRepository
import dev.franz.cli.kafka.repository.fake.EmptyBrokerRepository
import dev.franz.cli.kafka.repository.fake.EmptyClusterRepository
import dev.franz.cli.kafka.repository.fake.EmptyGroupRepository
import dev.franz.cli.kafka.repository.fake.EmptyTopicRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AclCommandsTest {
    
    private lateinit var aclRepository: AclRepository
    private lateinit var kafkaService: KafkaService
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setUp() {
        aclRepository = mockk()
        kafkaService = KafkaService(
            topics = EmptyTopicRepository(),
            brokers = EmptyBrokerRepository(),
            groups = EmptyGroupRepository(),
            acls = aclRepository,
            cluster = EmptyClusterRepository()
        )
        KafkaService.setInstance(kafkaService)
        
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        KafkaService.resetInstance()
    }
    
    @Test
    fun `GetAcl lists all ACLs`() {
        val acls = listOf(
            Acl("User:admin", ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL, AclOperation.ALL, AclPermission.ALLOW),
            Acl("User:producer", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        )
        every { aclRepository.listAcls(null, null, null) } returns acls
        
        GetAcl().main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("User:admin")
        assertThat(output).contains("User:producer")
        assertThat(output).contains("CLUSTER")
        assertThat(output).contains("TOPIC")
        verify { aclRepository.listAcls(null, null, null) }
    }
    
    @Test
    fun `GetAcl with principal filter`() {
        val acls = listOf(
            Acl("User:producer", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        )
        every { aclRepository.listAcls("User:producer", null, null) } returns acls
        
        GetAcl().main(arrayOf("--principal", "User:producer"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Principal: User:producer")
        assertThat(output).contains("User:producer")
        verify { aclRepository.listAcls("User:producer", null, null) }
    }
    
    @Test
    fun `GetAcl with resource type filter`() {
        val acls = listOf(
            Acl("User:producer", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        )
        every { aclRepository.listAcls(null, ResourceType.TOPIC, null) } returns acls
        
        GetAcl().main(arrayOf("--resource-type", "topic"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Resource Type: topic")
        verify { aclRepository.listAcls(null, ResourceType.TOPIC, null) }
    }
    
    @Test
    fun `CreateAcl creates new ACL`() {
        val acl = Acl("User:test-app", ResourceType.TOPIC, "my-topic", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        every { 
            aclRepository.createAcl("User:test-app", ResourceType.TOPIC, "my-topic", AclOperation.WRITE, AclPermission.ALLOW, PatternType.LITERAL) 
        } returns acl
        
        CreateAcl().main(arrayOf(
            "--principal", "User:test-app",
            "--resource-name", "my-topic",
            "--operation", "Write"
        ))
        
        val output = outputStream.toString()
        assertThat(output).contains("Creating ACL...")
        assertThat(output).contains("Principal:      User:test-app")
        assertThat(output).contains("Resource Name:  my-topic")
        assertThat(output).contains("Operation:      WRITE")
        assertThat(output).contains("ACL created successfully")
        verify { aclRepository.createAcl("User:test-app", ResourceType.TOPIC, "my-topic", AclOperation.WRITE, AclPermission.ALLOW, PatternType.LITERAL) }
    }
    
    @Test
    fun `CreateAcl with all options`() {
        val acl = Acl("User:app", ResourceType.GROUP, "my-group", PatternType.PREFIXED, AclOperation.READ, AclPermission.DENY)
        every { 
            aclRepository.createAcl("User:app", ResourceType.GROUP, "my-group", AclOperation.READ, AclPermission.DENY, PatternType.PREFIXED) 
        } returns acl
        
        CreateAcl().main(arrayOf(
            "--principal", "User:app",
            "--resource-type", "group",
            "--resource-name", "my-group",
            "--operation", "Read",
            "--permission", "Deny",
            "--pattern-type", "prefixed"
        ))
        
        val output = outputStream.toString()
        assertThat(output).contains("Resource Type:  group")
        assertThat(output).contains("Pattern Type:   prefixed")
        assertThat(output).contains("Permission:     DENY")
    }
    
    @Test
    fun `DeleteAcl without force shows confirmation`() {
        val acls = listOf(
            Acl("User:producer", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        )
        every { aclRepository.listAcls("User:producer", null, null) } returns acls
        
        DeleteAcl().main(arrayOf("--principal", "User:producer"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Found 1 matching ACLs")
        assertThat(output).contains("Use --force to confirm deletion")
    }
    
    @Test
    fun `DeleteAcl with force deletes ACLs`() {
        val acls = listOf(
            Acl("User:producer", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW)
        )
        every { aclRepository.listAcls("User:producer", null, null) } returns acls
        every { aclRepository.deleteAcls("User:producer", null, null, null) } returns acls
        
        DeleteAcl().main(arrayOf("--principal", "User:producer", "--force"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Deleted 1 ACLs")
        verify { aclRepository.deleteAcls("User:producer", null, null, null) }
    }
}
