#!/bin/bash

# Directory containing the manifests
MANIFEST_DIR="./k8s-deployments"

# Array of directories to apply YAML files from in specific order
DIRS_TO_APPLY=(
    "10-database"          # Start Database first
    "20-account"           # Start Accounts
    "30-people"            # Start People
    "40-position"          # Start Position
    "50-reference"         # Start Reference
    "60-trade-feed"        # Start the Trade feed
    "70-trade-processor"   # Start the trade processor
    "80-trade-service"     # Start the Trade Service
    "90-frontend"          # Start the Front end
    "100-ingress-loadgen"          # Enable ingress last
)

# Loop through each directory in the specified order
for DIR in "${DIRS_TO_APPLY[@]}"; do
    DIR_PATH="${MANIFEST_DIR}/${DIR}"
    if [[ -d "$DIR_PATH" ]]; then
        echo "Processing directory ${DIR_PATH}..."
        # Find and apply all .yaml or .yml files in the directory
        find "$DIR_PATH" -type f \( -name "*.yaml" -o -name "*.yml" \) | while read -r FILE; do
            echo "Applying ${FILE}..."
            kubectl apply -f "$FILE"
        done
    else
        echo "Warning: Directory ${DIR_PATH} not found!"
    fi
done

echo "All YAML manifests applied from the specified directories."

