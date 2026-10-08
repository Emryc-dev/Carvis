from fastapi import APIRouter

from app.api.v1 import admin, favorites, garage, scans, users, vehicles

router = APIRouter()
router.include_router(users.router)
router.include_router(vehicles.router)
router.include_router(scans.router)
router.include_router(favorites.router)
router.include_router(garage.router)
router.include_router(admin.router)

