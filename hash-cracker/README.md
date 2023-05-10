# Hash cracker

Distributed system to crack hashes. Consists of one **manager** service and multiple **worker** services.


# Build and run

Run in Docker Compose:
```bash
docker compose -f docker/docker-compose.yml up --build
```

# Testing

Send task to crack hash:
```bash
MSG="abcd"
HASH=$(echo -n ${MSG} | md5sum | awk '{print $1}')
REQUEST_ID=$(curl -s -X POST -H "Content-Type: application/json" -d '{"hash": "'${HASH}'", "maxLength": '${#MSG}'}' localhost:8080/api/hash/crack)
echo $REQUEST_ID
```

Check task status:
```bash
curl -s "localhost:8080/api/hash/status?requestId=${REQUEST_ID}"
```

Should return the following object:
```json
{
   "status":"READY",
   "data": ["abcd"]
}
```