FROM mcr.microsoft.com/azure-sql-edge:latest

EXPOSE 1433
ENV SA_PASSWORD "MyP@w0rd"
ENV SQLCMDPASSWORD "MyP@w0rd"
ENV ACCEPT_EULA "Y"

RUN (mkdir -p /opt/mssql-tools/bin && cd /opt/mssql-tools/bin && wget https://github.com/microsoft/go-sqlcmd/releases/download/v0.8.0/sqlcmd-v0.8.0-linux-arm64.tar.bz2 \
    && bzip2 -d sqlcmd-v0.8.0-linux-arm64.tar.bz2 && tar -xvf sqlcmd-v0.8.0-linux-arm64.tar && chmod 755 sqlcmd)

RUN  /opt/mssql/bin/sqlservr & sleep 20 && \
     /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -d master \
     -Q "create database localdb;" && \
     /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -d master -i  /database/schema.sql


CMD /opt/mssql/bin/sqlservr