import time
import random
import json
import urllib.request
import urllib.error
import os

API_URL = os.environ.get("API_URL", "http://localhost:8080/api/v1/events")
TENANTS = ["tenant_1", "tenant_2", "tenant_3"]
METRICS = ["cpu_utilization", "request_count", "error_rate"]

def send_event(tenant_id, event_type, value):
    payload = {
        "tenantId": tenant_id,
        "eventType": event_type,
        "value": float(value),
        "timestamp": int(time.time() * 1000)
    }
    
    req = urllib.request.Request(
        API_URL, 
        data=json.dumps(payload).encode('utf-8'),
        headers={
            'Content-Type': 'application/json',
            'X-Tenant-ID': tenant_id
        },
        method='POST'
    )
    
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            if response.status in (200, 201, 202):
                print(f"[SUCCESS] Sent {event_type}={value:.2f} for {tenant_id}", flush=True)
    except Exception as e:
        print(f"[ERROR] Failed to send event to {API_URL}: {e}", flush=True)

def main():
    print("====================================================", flush=True)
    print("PulseMetrics Real-Time Event Load Simulator Starting", flush=True)
    print(f"Target Endpoint: {API_URL}", flush=True)
    print("Generating simulated loads with periodic anomaly spikes...", flush=True)
    print("====================================================", flush=True)
    
    iteration = 0
    while True:
        iteration += 1
        is_spike = (iteration % 20 == 0) # Trigger a spike anomaly every 20 loops (~20 seconds)
        
        for tenant in TENANTS:
            for metric in METRICS:
                if metric == "cpu_utilization":
                    if is_spike and tenant == "tenant_1":
                        # Generate anomaly spike
                        value = random.uniform(95.0, 99.9)
                        print(f"\n[SPIKE TRIGGERED] Generating CPU utilization spike for {tenant}!", flush=True)
                    else:
                        # Normal load
                        value = random.uniform(35.0, 55.0)
                        
                elif metric == "request_count":
                    if is_spike and tenant == "tenant_2":
                        # Generate anomaly spike
                        value = random.randint(700, 1000)
                        print(f"\n[SPIKE TRIGGERED] Generating request count spike for {tenant}!", flush=True)
                    else:
                        # Normal load
                        value = random.randint(80, 150)
                        
                elif metric == "error_rate":
                    if is_spike and tenant == "tenant_3":
                        # Generate anomaly spike
                        value = random.uniform(30.0, 45.0)
                        print(f"\n[SPIKE TRIGGERED] Generating error rate spike for {tenant}!", flush=True)
                    else:
                        # Normal load
                        value = random.uniform(0.1, 2.5)
                
                send_event(tenant, metric, value)
                time.sleep(0.1) # Small delay between events
                
        time.sleep(1.0) # Wait 1 second before the next batch

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nSimulator stopped by user.", flush=True)
