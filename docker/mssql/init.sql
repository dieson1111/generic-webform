-- Create the database (run as SA or equivalent)
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'formdb')
BEGIN
    CREATE DATABASE formdb;
END
GO

USE formdb;
GO

-- Create the schema required by the application
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'form_system')
BEGIN
    EXEC('CREATE SCHEMA form_system');
END
GO
