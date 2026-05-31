package com.tcs.Machcare.filter;

import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.util.Jwtutil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Jwtutil jwtUtil;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(Jwtutil jwtUtil, EmployeeRepository employeeRepository) {
        this.jwtUtil = jwtUtil;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Long empId = jwtUtil.extractEmpId(authorization);
                Employee employee = employeeRepository.findById(empId).orElse(null);

                if (employee != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String roleName = switch (employee.getRoleId()) {
                        case 1 -> "ROLE_ADMIN";
                        case 2 -> "ROLE_ENGINEER";
                        case 3 -> "ROLE_OPERATOR";
                        default -> "ROLE_USER";
                    };

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    String.valueOf(employee.getEmpId()),
                                    null,
                                    List.of(new SimpleGrantedAuthority(roleName))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Let the security chain reject unauthorized requests.
            }
        }

        filterChain.doFilter(request, response);
    }
}