-- Defensa en profundidad multi-tenant: la BD rechaza que una fila apunte a un socio, plan o usuario de otro gym,
-- aunque un bug de la aplicación o una query nativa lo intentara.

ALTER TABLE app_user        ADD CONSTRAINT uk_app_user_gym_id UNIQUE (gym_id, id);
ALTER TABLE member          ADD CONSTRAINT uk_member_gym_id   UNIQUE (gym_id, id);
ALTER TABLE membership_plan ADD CONSTRAINT uk_plan_gym_id     UNIQUE (gym_id, id);

ALTER TABLE membership
    DROP CONSTRAINT membership_member_id_fkey,
    DROP CONSTRAINT membership_plan_id_fkey,
    DROP CONSTRAINT membership_created_by_fkey,
    ADD CONSTRAINT fk_membership_member     FOREIGN KEY (gym_id, member_id)  REFERENCES member (gym_id, id),
    ADD CONSTRAINT fk_membership_plan       FOREIGN KEY (gym_id, plan_id)    REFERENCES membership_plan (gym_id, id),
    ADD CONSTRAINT fk_membership_created_by FOREIGN KEY (gym_id, created_by) REFERENCES app_user (gym_id, id);

ALTER TABLE refresh_token
    DROP CONSTRAINT refresh_token_user_id_fkey,
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (gym_id, user_id) REFERENCES app_user (gym_id, id);
