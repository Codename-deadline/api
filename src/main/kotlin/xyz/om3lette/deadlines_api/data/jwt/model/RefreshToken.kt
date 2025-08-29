package xyz.om3lette.deadlines_api.data.jwt.model

import jakarta.persistence.*
import xyz.om3lette.deadlines_api.data.user.model.User
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @SequenceGenerator(name = "token_seq", sequenceName = "token_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
    val id: Long = 0,

    @Column(unique = true)
    val jti: String,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val expiry: Instant,

    var revoked: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User
)