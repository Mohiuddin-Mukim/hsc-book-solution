-- Phase 8/9: public student registration
-- Registration never accepts a client-selected role. Every public registration becomes STUDENT.
INSERT INTO role(code,name)
VALUES ('STUDENT','Student')
ON CONFLICT(code) DO NOTHING;
