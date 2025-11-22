FROM postgres:17.5

# Install build dependencies
RUN apt-get update && \
    apt-get install -y \
    build-essential \
    git \
    postgresql-server-dev-17 \
    && rm -rf /var/lib/apt/lists/*

# Install pg_cron
RUN cd /tmp && \
    git clone https://github.com/citusdata/pg_cron.git && \
    cd pg_cron && \
    make && \
    make install && \
    cd .. && \
    rm -rf pg_cron

# Install pgvector with ARM64 support
RUN cd /tmp && \
    git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git && \
    cd pgvector && \
    make OPTFLAGS="" && \
    make install && \
    cd .. && \
    rm -rf pgvector

# Add pg_cron to shared_preload_libraries
RUN echo "shared_preload_libraries = 'pg_cron'" >> /usr/share/postgresql/postgresql.conf.sample