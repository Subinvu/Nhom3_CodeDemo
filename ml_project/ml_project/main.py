from fastapi import FastAPI, Request, HTTPException
import joblib
import numpy as np
import pandas as pd
import uvicorn
import logging
from typing import Any, Dict

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()


# Try to load required artifacts on import; if missing we'll raise at request time
def _safe_load(path: str):
    try:
        return joblib.load(path)
    except Exception as e:
        logger.error("Không thể load %s: %s", path, e)
        return None


scaler = _safe_load('scaler.joblib')
kmeans = _safe_load('kmeans.joblib')
label_map = _safe_load('label_map.joblib') or {}


def _extract_rfm(body: Dict[str, Any]) -> Dict[str, float]:
    """Extract R, F, M from request body case-insensitively.

    Accepts keys like 'R' or 'r' or 'recency', 'F' or 'f' or 'frequency',
    'M' or 'm' or 'monetary'. Falls back to 0 for missing/invalid values.
    """
    if not isinstance(body, dict):
        return {'R': 0.0, 'F': 0.0, 'M': 0.0}

    lower_map = {str(k).lower(): v for k, v in body.items()}

    def _get_number(*keys, default=0.0):
        for k in keys:
            if k in lower_map:
                try:
                    return float(lower_map[k])
                except Exception:
                    return default
        return default

    R = _get_number('r', 'recency')
    F = _get_number('f', 'frequency')
    M = _get_number('m', 'monetary')
    return {'R': R, 'F': F, 'M': M}


@app.post('/predict')
async def predict(request: Request):
    # Ensure models are loaded
    if scaler is None or kmeans is None:
        logger.error('Model artifacts missing (scaler/kmeans).')
        raise HTTPException(status_code=500, detail='Model artifacts not available')

    try:
        body = await request.json()
    except Exception:
        # Gracefully handle non-JSON or empty bodies coming from Swagger/UI
        body = {}
    rfm = _extract_rfm(body)

    # Build DataFrame with same column names used during training
    df = pd.DataFrame({
        'Recency': [rfm['R']],
        'Frequency': [rfm['F']],
        'Monetary': [rfm['M']]
    })

    # Apply same log transform as during training
    try:
        log_df = np.log1p(df)
    except Exception as e:
        logger.exception('Log transform failed: %s', e)
        raise HTTPException(status_code=400, detail='Invalid numeric input')

    # Scale using loaded scaler
    try:
        scaled = scaler.transform(log_df)
    except Exception as e:
        logger.exception('Scaler transform failed: %s', e)
        raise HTTPException(status_code=500, detail='Scaling failed')

    # Predict cluster
    try:
        cluster = int(kmeans.predict(scaled)[0])
    except Exception as e:
        logger.exception('Prediction failed: %s', e)
        raise HTTPException(status_code=500, detail='Prediction failed')

    cluster_name = label_map.get(cluster, 'Khác')

    return {
        'cluster_id': cluster,
        'cluster_name': f'Cụm {cluster_name}'
    }


if __name__ == '__main__':
    uvicorn.run('main:app', host='0.0.0.0', port=8000)