import pytest
import os
import tempfile
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from fastapi.testclient import TestClient

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.schemas.auth import RegisterRequest, LoginRequest
from app.services.auth_service import AuthService

def test_sqlite_persistence_across_sessions():
    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        db_path = tmp.name

    db_url = f"sqlite:///{db_path}"
    engine = create_engine(db_url, connect_args={"check_same_thread": False})
    SessionMaker = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    
    # 1. First run: create tables & register user
    Base.metadata.create_all(bind=engine)
    db1 = SessionMaker()
    reg_req = RegisterRequest(
        full_name="Persistent User",
        email="persist@test.com",
        password="SecurePassword123!",
    )
    tokens = AuthService.register_user(db1, reg_req)
    assert tokens.user.email == "persist@test.com"
    db1.close()
    engine.dispose()

    # 2. Simulate server restart: new engine & session connecting to same sqlite file
    engine2 = create_engine(db_url, connect_args={"check_same_thread": False})
    SessionMaker2 = sessionmaker(autocommit=False, autoflush=False, bind=engine2)
    db2 = SessionMaker2()
    
    # Login must succeed because data persisted in SQLite file
    login_req = LoginRequest(
        email="persist@test.com",
        password="SecurePassword123!",
    )
    login_tokens = AuthService.authenticate_user(db2, login_req)
    assert login_tokens.user.email == "persist@test.com"
    assert login_tokens.access_token is not None

    db2.close()
    engine2.dispose()

    # Cleanup temp db
    if os.path.exists(db_path):
        os.remove(db_path)
