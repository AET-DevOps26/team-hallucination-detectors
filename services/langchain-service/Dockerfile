FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt
# opentelemetry-bootstrap imports pkg_resources, which recent setuptools no
# longer ships by default — pin an older one just for the bootstrap step.
RUN pip install --no-cache-dir "setuptools<81"
# Installs auto-instrumentation for whatever's already in requirements.txt
# (fastapi, httpx, etc.) so incoming/outgoing spans get created without
# hand-writing tracing code, and propagates trace context from callers
# (the Java services' OTel agent) automatically.
RUN opentelemetry-bootstrap -a install

COPY app ./app

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8000

CMD ["opentelemetry-instrument", "python", "-m", "app.server"]