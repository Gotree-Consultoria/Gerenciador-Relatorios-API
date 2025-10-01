package com.gotree.API.mappers;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.entities.User;

import br.com.caelum.stella.format.CPFFormatter;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", expression = "java(com.gotree.API.entities.enums.UserRole.USER)")
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserRequestDTO dto);


    UserResponseDTO toDto(User user);

    List<UserResponseDTO> toDtoList(List<User> user);

    // Só formata quando vai grava (DTO -> Entity)
    @AfterMapping
    default void formatCpfAfterToEntity(@MappingTarget User user) {
        if (user.getCpf() != null) {
            String cleanCpf = user.getCpf().replaceAll("[^\\d]", "");
            CPFFormatter formatter = new CPFFormatter();
            user.setCpf(formatter.format(cleanCpf));
        }

    }

}
