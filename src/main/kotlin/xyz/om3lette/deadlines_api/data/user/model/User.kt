package xyz.om3lette.deadlines_api.data.user.model

import jakarta.persistence.*
import jakarta.validation.constraints.Size
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import java.time.Instant

@Entity
@Table(name = "users")
data class User(
    @Id
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    val id: Long = 0,

    @Column(unique = true, name = "username")
    val _username: String,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val joinedAt: Instant,

    @field:Size(min = 2, max = 128, message = "FullName must be between 2 and 128 characters")
    var fullName: String,

    @Column(name = "password")
    var _password: String? = null,

    @Enumerated(EnumType.STRING)
    var language: Language = Language.EN,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var lastPasswordChange: Instant = Instant.EPOCH,

    @Enumerated(value = EnumType.STRING)
    val role: UserRole = UserRole.USER,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn
    val messengerAccounts: MutableList<UserMessengerAccount> = mutableListOf()

) : UserDetails {
    override fun getAuthorities(): MutableCollection<GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority(role.name))

    override fun getPassword() = _password

    override fun getUsername() = _username

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = true

    fun toMap() = mapOf(
        "id" to id,
        "username" to _username,
        "fullName" to fullName,
        "joinedAt" to joinedAt
    )
}
