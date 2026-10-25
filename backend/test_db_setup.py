from contextlib import contextmanager
from pathlib import Path
from tempfile import TemporaryDirectory

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from database import Base
from main import app, get_db


@contextmanager
def isolated_test_client():
    with TemporaryDirectory() as folder:
        db_path = Path(folder) / "test_trading.db"
        engine = create_engine(
            f"sqlite:///{db_path.as_posix()}",
            connect_args={"check_same_thread": False}
        )

        Base.metadata.create_all(bind=engine)

        TestSession = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=engine
        )

        def override_get_db():
            with TestSession() as db:
                yield db

        app.dependency_overrides[get_db] = override_get_db

        try:
            with TestClient(app) as client:
                yield client, TestSession
        finally:
            app.dependency_overrides.pop(get_db, None)
            engine.dispose()