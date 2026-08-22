# Manual Deployment Guide (No Docker)

This document outlines the requirements and step-by-step instructions for deploying the application (Next.js frontend, Spring Boot backend, and PostgreSQL) directly on a bare-metal server or VM without using Docker.

## 1. Server Requirements
- **OS**: Ubuntu 22.04 LTS (recommended).
- **RAM**: Minimum 2GB (4GB+ recommended).
- **CPU**: 2 Cores recommended.
- **Network**: Ports `80` (HTTP), `443` (HTTPS) and `22` (SSH) open to the internet.

---

## 2. Server Software Installation

You will need to install the core dependencies for both the frontend and backend directly on the server's OS.

### A. Java 17 (For Spring Boot Backend)
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version # Verify installation
```

### B. Node.js (For Next.js Frontend)
We recommend installing Node v20 via NodeSource.
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v # Verify installation
```

### C. PM2 (Process Manager)
PM2 will be used to keep the Next.js frontend running in the background persistently.
```bash
sudo npm install -g pm2
```

### D. PostgreSQL (Database)
Install the database and set it up to start on boot.
```bash
sudo apt install postgresql postgresql-contrib -y
sudo systemctl enable --now postgresql
```
The database, login, and `play_neml` schema are company-managed. Do not create or alter databases, schemas, users, roles, or extensions. Request connection details and approved `play_neml` permissions from the company DBA.

### E. Nginx (Reverse Proxy)
Nginx is used to route web traffic on port 80/443 to your Next.js frontend and Spring Boot backend.
```bash
sudo apt install nginx -y
sudo systemctl enable --now nginx
```

---

## 3. Deployment Steps

### Step 1: Clone the Repository
Pull your code onto the server:
```bash
git clone <your-repository-url>
cd sports-play-main
```

### Step 2: Deploy Backend (Spring Boot)
1. **Configure application properties:**
   Ensure your backend uses the correct database credentials. You can pass them as environment variables or update `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://<company-host>:5432/<company-database>?currentSchema=play_neml
   spring.datasource.username=<company-provided-user>
   spring.datasource.password=<company-provided-password>
   spring.jpa.properties.hibernate.default_schema=play_neml
   spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=false
   ```

2. **Build the JAR file:**
   Navigate into the backend folder and build the executable JAR (assuming Maven is used):
   ```bash
   cd backend
   ./mvnw clean package -DskipTests
   ```

3. **Run the Backend (using systemd):**
   To keep the backend running in the background automatically, create a systemd service file:
   ```bash
   sudo nano /etc/systemd/system/badminton-backend.service
   ```
   *Paste the following (adjust the path to match your actual server path):*
   ```ini
   [Unit]
   Description=Badminton Spring Boot App
   After=syslog.target
   After=network.target

   [Service]
   User=root
   Type=simple
   ExecStart=/usr/bin/java -jar /root/sports-play-main/backend/target/badminton-0.0.1-SNAPSHOT.jar
   Restart=always
   StandardOutput=syslog
   StandardError=syslog
   SyslogIdentifier=badminton-backend

   [Install]
   WantedBy=multi-user.target
   ```
   Start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now badminton-backend
   ```

### Step 3: Deploy Frontend (Next.js)
1. **Configure Environment Variables:**
   Create an environment file for production in the frontend directory:
   ```bash
   cd ../frontend
   nano .env.production
   ```
   *Add the backend URL:*
   ```env
   NEXT_PUBLIC_API_URL=http://your_domain_or_ip/api
   ```

2. **Install dependencies and build:**
   ```bash
   npm install
   npm run build
   ```

3. **Start with PM2:**
   Start the compiled Next.js application on port 3000:
   ```bash
   pm2 start npm --name "badminton-frontend" -- start
   pm2 save
   pm2 startup
   ```

### Step 4: Configure Nginx Reverse Proxy
Finally, configure Nginx to route internet traffic to your local services.
```bash
sudo nano /etc/nginx/sites-available/badminton
```
*Paste the following configuration:*
```nginx
server {
    listen 80;
    server_name your_domain_or_ip;

    # Route main traffic to Next.js frontend (running on port 3000)
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Route API requests to Spring Boot backend (running on port 8080)
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```
Enable the site and restart Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/badminton /etc/nginx/sites-enabled/
# Remove default nginx config to prevent conflicts
sudo rm /etc/nginx/sites-enabled/default
# Test syntax and restart
sudo nginx -t
sudo systemctl restart nginx
```

---

## 4. Maintenance Commands

- **Backend Logs (Spring Boot):**
  ```bash
  sudo journalctl -u badminton-backend -f
  ```
- **Frontend Logs (Next.js):**
  ```bash
  pm2 logs badminton-frontend
  ```
- **Restart Backend:**
  ```bash
  sudo systemctl restart badminton-backend
  ```
- **Restart Frontend:**
  ```bash
  pm2 restart badminton-frontend
  ```
