-- Fix template placeholders from $${ to ${ after disabling Flyway placeholder replacement
-- Now that placeholder-replacement is disabled, we can use single ${} for template variables

UPDATE prompt_templates
SET template_text = REPLACE(template_text, '$${', '${')
WHERE template_text LIKE '%$${%';
