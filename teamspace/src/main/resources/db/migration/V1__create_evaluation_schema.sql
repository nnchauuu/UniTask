CREATE TABLE IF NOT EXISTS evaluation_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    level VARCHAR(30) NOT NULL,
    system_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evaluation_template_name_system UNIQUE (name, system_template)
);

CREATE TABLE IF NOT EXISTS project_evaluation_configs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    source_template_id BIGINT,
    name VARCHAR(150) NOT NULL,
    updated_by_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_evaluation_config UNIQUE (project_id)
);

CREATE TABLE IF NOT EXISTS evaluation_criteria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT,
    project_config_id BIGINT,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    weight INT NOT NULL,
    evaluation_type VARCHAR(30) NOT NULL,
    metric_key VARCHAR(50),
    scale_max INT NOT NULL,
    manual_evaluator VARCHAR(30),
    requires_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    INDEX idx_evaluation_criteria_template (template_id),
    INDEX idx_evaluation_criteria_config (project_config_id)
);

CREATE TABLE IF NOT EXISTS evaluation_cycles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) NOT NULL,
    created_by_id BIGINT NOT NULL,
    finalized_by_id BIGINT,
    finalized_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_evaluation_cycles_project (project_id, created_at)
);

CREATE TABLE IF NOT EXISTS evaluation_cycle_criteria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cycle_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    weight INT NOT NULL,
    evaluation_type VARCHAR(30) NOT NULL,
    metric_key VARCHAR(50),
    scale_max INT NOT NULL,
    manual_evaluator VARCHAR(30),
    requires_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_cycle_criteria_cycle (cycle_id, sort_order)
);

CREATE TABLE IF NOT EXISTS member_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cycle_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    self_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    leader_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    finalized BOOLEAN NOT NULL DEFAULT FALSE,
    total_score DOUBLE,
    final_comment VARCHAR(1500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cycle_member_evaluation UNIQUE (cycle_id, member_id)
);

CREATE TABLE IF NOT EXISTS criterion_scores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_evaluation_id BIGINT NOT NULL,
    cycle_criterion_id BIGINT NOT NULL,
    auto_score DOUBLE,
    manual_score DOUBLE,
    final_score DOUBLE,
    insufficient_data BOOLEAN NOT NULL DEFAULT FALSE,
    comment VARCHAR(1500),
    PRIMARY KEY (id),
    CONSTRAINT uk_member_criterion_score UNIQUE (member_evaluation_id, cycle_criterion_id)
);

CREATE TABLE IF NOT EXISTS manual_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    criterion_score_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    evaluation_type VARCHAR(30) NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(1500),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_manual_evaluation_score (criterion_score_id)
);

CREATE TABLE IF NOT EXISTS evaluation_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    criterion_score_id BIGINT NOT NULL,
    evidence_type VARCHAR(30) NOT NULL,
    task_id BIGINT,
    file_id BIGINT,
    meeting_id BIGINT,
    note VARCHAR(1000),
    created_by_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_evaluation_evidence_score (criterion_score_id)
);
