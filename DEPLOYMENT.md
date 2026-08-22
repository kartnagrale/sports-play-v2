# Deployment Guide (Docker)

This document outlines all the requirements and steps needed to deploy this full-stack application (Next.js frontend, Spring Boot backend, and PostgreSQL database) on a remote server using Docker.

## 1. Server Requirements

To host the application, you will need a server (e.g., AWS EC2, DigitalOcean Droplet, Linode) with the following minimum specifications:
- **OS**: Ubuntu 22.04 LTS (recommended) or any modern Linux distribution.
- **RAM**: At least 2GB (4GB recommended for smooth running of Spring Boot, Next.js, and Postgres).
- **CPU**: 2 Cores recommended.
- **Network**: Port `80` / `443` open for web traffic, and SSH (`22`) for server access.

## 2. Server Software Installation

You must install Docker and Docker Compose on the server.

### Install Docker Engine
Run the following commands on your Ubuntu server:
```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install docker.io -y

# Enable and start Docker service
sudo systemctl enable --now docker

# Check if Docker is running
sudo systemctl status docker
```

### Install Docker Compose
Docker Compose is required to run multiple containers together.
```bash
# Install Docker Compose (if not included as a docker plugin)
sudo apt install docker-compose-v2 -y
# OR using the apt repository plugin:
sudo apt install docker-compose-plugin -y
```
Check installation:
```bash
docker compose version
```

## 3. Project Configuration Requirements

Before deploying, your project code must have the following configuration files added. *(Note: If these do not exist yet, they need to be created).*

1. **Backend Dockerfile** (`backend/Dockerfile`):
   - Needs to build the Java Spring Boot application (using Maven/Gradle) and run the generated `.jar` file using an OpenJDK image.
2. **Frontend Dockerfile** (`frontend/Dockerfile`):
   - Needs to build the Next.js application and serve it using a lightweight Node.js image.
3. **Docker Compose File** (`docker-compose.yml`):
   - Needs to be in the root directory.
   - Should define 3 services: `frontend`, `backend`, and `db` (PostgreSQL).
   - Should set up networking so the containers can communicate.
   - Should configure volumes for PostgreSQL data persistence (so you don't lose data on restart).

## 4. Environment Variables

You will need to set up environment variables on the server. Usually, these are stored in a `.env` file in the root of the project on the server.

**Example `.env` structure:**
```env
# Database configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=badminton_db

# Backend configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/badminton_db
JWT_SECRET=your_super_secret_jwt_key_here

# Frontend configuration
NEXT_PUBLIC_API_URL=http://your-server-ip-or-domain/api
```

## 5. Deployment Steps

Once your server is provisioned and your code is pushed to a Git repository (like GitHub/GitLab), follow these steps on the server:

1. **Clone the repository:**
   ```bash
   git clone <your-repository-url>
   cd sports-play-main
   ```

2. **Create the `.env` file:**
   ```bash
   nano .env
   # Add your production environment variables here, then save and exit
   ```

3. **Build and start the containers:**
   ```bash
   # The -d flag runs the containers in the background
   sudo docker compose up -d --build
   ```

4. **Verify Deployment:**
   - Run `sudo docker compose ps` to check if all containers are 'Up'.
   - Visit your server's IP address or domain in a browser.

## 6. Maintenance Commands

- **View application logs:**
  ```bash
  sudo docker compose logs -f
  ```
  *(To view logs for a specific service, append the service name: `sudo docker compose logs -f backend`)*

- **Stop the application:**
  ```bash
  sudo docker compose down
  ```

- **Restart the application:**
  ```bash
  sudo docker compose restart
  ```

- **Update the application (after git pull):**
  ```bash
  git pull origin main
  sudo docker compose up -d --build
  ```
