.PHONY: format
format:
	@echo "Formatting code..."
	mvn spotless:apply
	@echo "Code formatting complete"
	
clean:
	@echo "Cleaning up containers and images..."
	docker-compose down -v || true
	docker-compose -f docker-compose.test.yml down -v || true
	docker system prune -f || true

test:
	@echo "Running tests..."
	docker-compose -f docker-compose.test.yml up \
		--abort-on-container-exit
	docker-compose -f docker-compose.test.yml down -v
	@echo "Tests completed!"

run:
	@echo "Starting application..."
	docker-compose up -d --build
	@echo "Application is running!"

stop:
	docker-compose down

cli:
	docker-compose exec app java -jar app.jar