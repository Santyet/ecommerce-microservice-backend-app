package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    private static final String TEST_FIRST_NAME = "Lucía";
    private static final String TEST_LAST_NAME = "Martínez";
    private static final String TEST_EMAIL = "lucia.martinez@correo.com";
    private static final String TEST_PHONE = "3001112233";
    private static final String TEST_IMAGE_URL = "http://fotos.com/lucia.jpg";
    private static final String TEST_USERNAME = "lucia_mtz";
    private static final String TEST_PASSWORD = "secreta321";

    @BeforeEach
    void setUp() {
        Credential credential = Credential.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        user = User.builder()
                .userId(42)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .email(TEST_EMAIL)
                .phone(TEST_PHONE)
                .imageUrl(TEST_IMAGE_URL)
                .credential(credential)
                .build();

        credential.setUser(user);
    }
    @Test
    void findAll_shouldReturnListOfUserDtos() {
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        List<UserDto> result = userService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_FIRST_NAME, result.get(0).getFirstName());
        verify(userRepository, times(1)).findAll();
    }
    @Test
    void findById_shouldReturnUserDto_whenExists() {
        when(userRepository.findById(42)).thenReturn(Optional.of(user));

        UserDto result = userService.findById(42);

        assertNotNull(result);
        assertEquals(42, result.getUserId());
        assertEquals(TEST_FIRST_NAME, result.getFirstName());
        verify(userRepository, times(1)).findById(42);
    }
    @Test
    void findById_shouldThrowException_whenNotExists() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> userService.findById(999));
        verify(userRepository, times(1)).findById(999);
    }
    @Test
    void save_shouldReturnSavedUserDto() {
        UserDto userDto = UserMappingHelper.map(user);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.save(userDto);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getCredential().getUsername(), result.getCredentialDto().getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }
    @Test
    void deleteById_shouldCallRepositoryDelete() {
        doNothing().when(userRepository).deleteById(42);

        userService.deleteById(42);

        verify(userRepository, times(1)).deleteById(42);
    }
}
