from typing import Any, Dict, Optional, Tuple

from flask import jsonify, request, Response, abort
from flask.views import MethodView
from marshmallow import ValidationError
from sqlalchemy.exc import NoResultFound

from util.admin_api_key import admin_api_key_required
from models import db
from models.users import User
from schemas.user import user_schema, users_schema
from services.user_deletion import delete_user


class UserAPI(MethodView):
    @staticmethod
    def _fetch_users(filter_by: Optional[Dict[str, Any]] = None) -> Tuple[Response, int]:
        try:
            query = db.session.query(User)
            if filter_by:
                user = query.filter_by(**filter_by).one()
                result = user_schema.dump(user)
            else:
                users = query.all()
                result = users_schema.dump(users)
            return jsonify(result), 200
        except NoResultFound:
            return jsonify({'error': 'User not found'}), 404
        except Exception:
            return UserAPI._internal_server_error()

    @staticmethod
    def _get_user_by_id(_id: str) -> Optional[User]:
        return db.session.query(User).get(_id)

    @staticmethod
    def _internal_server_error() -> Tuple[Response, int]:
        return abort(500)

    @staticmethod
    def _handle_validation_error(err: ValidationError) -> Tuple[Response, int]:
        return jsonify(err.messages), 400

    @admin_api_key_required
    def get(self, _id: Optional[str] = None) -> Tuple[Response, int]:
        return self._fetch_users(filter_by={'id': _id} if _id else None)

    @admin_api_key_required
    def post(self) -> Tuple[Response, int]:
        try:
            user = User(**request.get_json())
            db.session.add(user)
            db.session.commit()
            return jsonify(user_schema.dump(user)), 201
        except Exception:
            db.session.rollback()
            return self._internal_server_error()

    @admin_api_key_required
    def put(self, _id: str) -> Tuple[Response, int]:
        try:
            user = self._get_user_by_id(_id)
            if not user:
                return jsonify({'error': 'User not found'}), 404
            updated_data = user_schema.load(request.get_json(), partial=True)
            self._update_user(user, updated_data)
            return jsonify(user_schema.dump(user)), 200
            if cenas:
                return cenas
        except ValidationError as err:
            db.session.rollback()
            return self._handle_validation_error(err)
        except Exception:
            db.session.rollback()
            return self._internal_server_error()

    @staticmethod
    def _update_user(user: User, updated_data: Dict[str, Any]) -> None:
        for key, value in updated_data.items():
            setattr(user, key, value)
        db.session.commit()

    @admin_api_key_required
    def delete(self, _id: str) -> Tuple[Response, int]:
        try:
            delete_user(_id)
            return jsonify({}), 200
        except Exception:
            db.session.rollback()
            return self._internal_server_error()


admin_user_view = UserAPI.as_view('admin_user')