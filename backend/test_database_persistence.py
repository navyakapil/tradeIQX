import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine, text


class TestDatabasePersistence(unittest.TestCase):

    def test_data_survives_reconnection(self):
        with TemporaryDirectory() as folder:
            db_path = Path(folder) / "test_persistence.db"
            db_url = f"sqlite:///{db_path.as_posix()}"

            engine = create_engine(db_url)

            with engine.begin() as connection:
                connection.execute(
                    text(
                        "CREATE TABLE test_portfolio "
                        "(id INTEGER PRIMARY KEY, cash_balance FLOAT)"
                    )
                )
                connection.execute(
                    text(
                        "INSERT INTO test_portfolio "
                        "(id, cash_balance) VALUES (1, 97000)"
                    )
                )

            engine.dispose()

            new_engine = create_engine(db_url)

            try:
                with new_engine.connect() as connection:
                    balance = connection.execute(
                        text(
                            "SELECT cash_balance FROM "
                            "test_portfolio WHERE id = 1"
                        )
                    ).scalar_one()

                self.assertEqual(balance, 97000)
            finally:
                new_engine.dispose()


if __name__ == "__main__":
    unittest.main()