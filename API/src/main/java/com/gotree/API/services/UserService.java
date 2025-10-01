package com.gotree.API.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Importe Optional

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.user.BatchUserInsertResponseDTO;
import com.gotree.API.dto.user.FailedUserDTO;
import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.dto.user.UserUpdateDTO;
import com.gotree.API.entities.User;
import com.gotree.API.exceptions.CpfValidationException;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.UserMapper;
import com.gotree.API.repositories.UserRepository;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;

@Service
public class UserService implements UserDetailsService {

    // region Dependencies
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    // endregion

    // region CRUD Operations

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + id + " não encontrado."));
    }

    // Método público para encontrar um usuário por email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User insertUser(UserRequestDTO dto) {
        validateUser(dto);
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID: " + id + " não encontrado."));

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário com ID: " + id + "não encontrado");
        }
        userRepository.deleteById(id);
    }

    // Logica pra resetar a senha (fazendo com que o username vire a senha email ==
    // senha
    public void resetPassword(Long userId) {
        User user = findById(userId);

        String newPassword = user.getEmail();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetRequired(true);


        userRepository.save(user);
    }

    // endregion

    // region Batch Insert

    public BatchUserInsertResponseDTO insertUsers(List<UserRequestDTO> userDTOs) {
        List<UserResponseDTO> successUsers = new ArrayList<>();
        List<FailedUserDTO> failedUsers = new ArrayList<>();

        for (UserRequestDTO dto : userDTOs) {
            try {
                validateUser(dto);
                User user = userMapper.toEntity(dto);
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                User saved = userRepository.save(user);
                successUsers.add(userMapper.toDto(saved));
            } catch (Exception e) {
                failedUsers.add(new FailedUserDTO(dto.getEmail(), e.getMessage()));
            }
        }

        return new BatchUserInsertResponseDTO(successUsers, failedUsers);
    }

    // endregion

    // region Validation

    public void validateUser(UserRequestDTO userDTO) {
        userRepository.findByEmail(userDTO.getEmail()).ifPresent(u -> {
            throw new DataIntegrityViolationException("Email já cadastrado: " + u.getEmail());
        });

        String cleanCpf = userDTO.getCpf().replaceAll("[^\\d]", "");
        CPFValidator cpfValidator = new CPFValidator();

        try {
            cpfValidator.assertValid(cleanCpf);
        } catch (InvalidStateException e) {
            throw new CpfValidationException("CPF inválido: " + userDTO.getCpf());
        }
    }

    // endregion

    // region Spring Security Integration

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("Buscando usuário por email: " + email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return new CustomUserDetails(user);
    }

    // endregion
}
