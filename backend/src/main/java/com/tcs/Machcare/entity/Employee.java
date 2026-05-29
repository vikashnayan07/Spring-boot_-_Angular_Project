package com.tcs.Machcare.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee", schema = "dev")
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long empId;
    
    private String name;
    
    private String email;
    
    @Column(name = "role_id")
    private Integer roleId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role_name")
    private RoleType roleName;

    // CHANGED: Using Boolean object wrapper instead of primitive boolean 
    // to prevent crashes if the database contains NULL values.
    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "suspension_end_date")
    private LocalDateTime suspensionEndDate;
    
    
    
 // 👉 NEW FIELDS FOR FIRST-TIME SETUP
    @Column(name = "is_first_login")
    private Boolean isFirstLogin = true; // Defaults to true for all new employees

    private String securityQuestion1;
    private String securityAnswer1;
    private String securityQuestion2;
    private String securityAnswer2;

    // (Don't forget to generate Getters and Setters for these at the bottom of your file!)

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getEmpId() { 
        return empId; 
    }
    
    public void setEmpId(Long empId) { 
        this.empId = empId; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public Integer getRoleId() { 
        return roleId; 
    }
    
    public void setRoleId(Integer roleId) { 
        this.roleId = roleId; 
    }
    
    public RoleType getRoleName() { 
        return roleName; 
    }
    
    public void setRoleName(RoleType roleName) { 
        this.roleName = roleName; 
    }
    
    // CHANGED: Null-safe getter. If an old database row has NULL, it defaults to true.
    public boolean isActive() { 
        return this.isActive != null ? this.isActive : true; 
    }
    
    public void setActive(Boolean active) { 
        this.isActive = active; 
    }
    
    public LocalDateTime getSuspensionEndDate() { 
        return suspensionEndDate; 
    }
    
    public void setSuspensionEndDate(LocalDateTime suspensionEndDate) { 
        this.suspensionEndDate = suspensionEndDate; 
    }

	public Object getIsFirstLogin() {
		// TODO Auto-generated method stub
		return isFirstLogin;
	}
	
	public void setIsFirstLogin(Boolean isFirstLogin) { 
        this.isFirstLogin = isFirstLogin; 
    }
}