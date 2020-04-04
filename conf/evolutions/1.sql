-- !Ups
CREATE TABLE PET (
    id           VARCHAR(36) PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    sex          ENUM('FEMALE', 'MALE') NOT NULL,
    birth_date   DATE,
    entry_date   DATE,
    release_date DATE,
    description  TEXT NOT NULL,
    created      TIMESTAMP NOT NULL,
    updated      TIMESTAMP,
    enable       BOOLEAN NOT NULL,
    UNIQUE (name, sex, birth_date, description)
);

CREATE TABLE PET_MEDIA (
	pet_id      VARCHAR(36) NOT NULL,
    media_type  ENUM('PHOTO', 'VIDEO') NOT NULL,
    url         TEXT NOT NULL,
    description TEXT,
    FOREIGN KEY (pet_id) REFERENCES PET (id)
);

CREATE TABLE PET_IDENTIFICATION (
    pet_id               VARCHAR(36) NOT NULL,
    identification_type  ENUM('TATTOO', 'CHIP') NOT NULL,
    identification_value TEXT NOT NULL,
    UNIQUE (pet_id, identification_type),
    FOREIGN KEY (pet_id) REFERENCES PET (id)
);

CREATE TABLE PET_INTERACTION (
    pet_id      VARCHAR(36) NOT NULL,
    specie      ENUM('HUMAN', 'CHILD', 'CAT', 'DOG') NOT NULL,
    interaction ENUM('PEACEFUL', 'SAME_SEX', 'IMPOSSIBLE') NOT NULL,
    UNIQUE (pet_id, specie),
    FOREIGN KEY (pet_id) REFERENCES PET (id)
);

CREATE TABLE PET_CONDITION (
    pet_id         VARCHAR(36) NOT NULL,
    home_condition ENUM('HOME', 'GARDEN', 'FLAT', 'FENCES') NOT NULL,
    UNIQUE (pet_id, home_condition),
    FOREIGN KEY (pet_id) REFERENCES PET (id)
);

CREATE TABLE MEDICAL_RECORD (
    id          VARCHAR(36) PRIMARY KEY,
    pet_id      VARCHAR(36) NOT NULL,
    record_type ENUM('VACCINE', 'DISEASE', 'CHECK', 'CARE') NOT NULL,
    record_date DATE NOT NULL,
    treatment   TEXT NOT NULL,
    description TEXT NOT NULL,
    done        BOOLEAN NOT NULL,
    created     TIMESTAMP NOT NULL,
    updated     TIMESTAMP,
    enable      BOOLEAN NOT NULL,
    UNIQUE (pet_id, record_type, record_date),
    FOREIGN KEY (pet_id) REFERENCES PET (id)
);

-- !Downs
DROP TABLE PET;
DROP TABLE PET_MEDIA;
DROP TABLE PET_IDENTIFICATION;
DROP TABLE PET_INTERACTION;
DROP TABLE PET_CONDITION;
DROP TABLE MEDICAL_RECORD;