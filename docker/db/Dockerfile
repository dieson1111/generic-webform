FROM postgres:15-alpine

# Copy the initialization script to the directory where standard PostgreSQL image 
# looks for scripts to run on startup.
COPY init.sql /docker-entrypoint-initdb.d/init.sql
