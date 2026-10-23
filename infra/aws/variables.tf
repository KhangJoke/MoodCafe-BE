variable "aws_region" {
  description = "Region gần Việt Nam; ưu đãi T4g hiện có tại Singapore."
  type        = string
  default     = "ap-southeast-1"
}

variable "project_name" {
  type    = string
  default = "moodcafe"
  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,19}$", var.project_name))
    error_message = "Tên project dài 3–20 ký tự, chỉ chữ thường, số và dấu gạch ngang."
  }
}

variable "instance_type" {
  type    = string
  default = "t4g.small"
  validation {
    condition     = contains(["t4g.small", "t4g.medium"], var.instance_type)
    error_message = "Module dùng ARM64; chọn t4g.small hoặc t4g.medium."
  }
}

variable "availability_zone" {
  description = "Để trống sẽ chọn AZ đầu tiên của region. Data volume cố định trong AZ này."
  type        = string
  default     = ""
}

variable "root_volume_gb" {
  type    = number
  default = 12
  validation {
    condition     = var.root_volume_gb >= 12 && floor(var.root_volume_gb) == var.root_volume_gb
    error_message = "Ổ hệ điều hành tối thiểu 12 GB."
  }
}

variable "data_volume_gb" {
  description = "Ổ riêng cho database, Redis, Caddy và file cấu hình ứng dụng."
  type        = number
  default     = 10
  validation {
    condition     = var.data_volume_gb >= 10 && floor(var.data_volume_gb) == var.data_volume_gb
    error_message = "Ổ dữ liệu tối thiểu 10 GB; AWS chỉ hỗ trợ tăng dung lượng trực tiếp."
  }
}

variable "ssh_cidr" {
  description = "Mặc định tắt SSH. Nếu cần: IP public của bạn dạng x.x.x.x/32."
  type        = string
  default     = ""
  validation {
    condition     = var.ssh_cidr == "" || (can(cidrnetmask(var.ssh_cidr)) && endswith(var.ssh_cidr, "/32"))
    error_message = "Chỉ cho phép một IPv4 /32, hoặc để trống để tắt SSH."
  }
}

variable "ssh_public_key" {
  description = "Public key OpenSSH; không đưa private key vào Terraform."
  type        = string
  default     = ""
  validation {
    condition     = var.ssh_public_key == "" || can(regex("^ssh-(ed25519|rsa) ", var.ssh_public_key))
    error_message = "Cung cấp public key ssh-ed25519/ssh-rsa, hoặc để trống."
  }
}

variable "domain_name" {
  description = "Domain BE nếu có. Trống = HTTP kiểm tra; có domain = Caddy cấp HTTPS sau khi DNS A trỏ tới Elastic IP."
  type        = string
  default     = ""
  validation {
    condition     = var.domain_name == "" || can(regex("^([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$", var.domain_name))
    error_message = "Domain viết thường, không chứa protocol, port hoặc đường dẫn."
  }
}

variable "enable_backups" {
  description = "Tạo bucket riêng và backup PostgreSQL hằng ngày, giữ 30 ngày."
  type        = bool
  default     = true
}

