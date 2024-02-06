package main.wonprice.domain.picture.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import main.wonprice.domain.member.entity.Member;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
@Getter
@Setter
@DiscriminatorValue("member")
public class MemberPicture extends Picture {

    @OneToOne
    @JoinColumn(name = "member_id")
    @JsonIgnore
    private Member member;
}
