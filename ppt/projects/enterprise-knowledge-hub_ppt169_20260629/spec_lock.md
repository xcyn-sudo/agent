# spec_lock — enterprise-knowledge-hub

mode: narrative
visual_style: blueprint
delivery_purpose: balanced

## colors

background: "#0D1117"
secondary_bg: "#1A1F36"
primary: "#1A73E8"
accent: "#00C853"
secondary_accent: "#7C4DFF"
body_text: "#E8ECF0"
secondary_text: "#9CA3AF"
tertiary_text: "#6B7280"
border_divider: "#2A3040"
success: "#00C853"
warning: "#FF1744"
surface: "#151B28"
grid: "#1E2640"
card_bg: "#151B28"
card_stroke: "#2A3040"
white_text: "#FFFFFF"
image_rendering: blueprint
image_palette: cool-corporate

## typography

body: 24
title: 42
subtitle: 32
annotation: 18
footnote: 16
lead: 30
cover_title: 72
hero_number: 48
subheading: 28
chart_annotation: 16
heading_family: "SimHei, Arial, \"Microsoft YaHei\", sans-serif"
body_family: "\"Microsoft YaHei\", \"PingFang SC\", Arial, sans-serif"
code_family: "Consolas, \"Courier New\", monospace"

## icons

library: phosphor-duotone
list:
  - phosphor-duotone/magnifying-glass
  - phosphor-duotone/database
  - phosphor-duotone/lock-laminated
  - phosphor-duotone/graph
  - phosphor-duotone/person-arms-spread
  - phosphor-duotone/file
  - phosphor-duotone/clock
  - phosphor-duotone/chart-bar
  - phosphor-duotone/gear-six
  - phosphor-duotone/eye
  - phosphor-duotone/link-simple
  - phosphor-duotone/cloud-arrow-up
  - phosphor-duotone/chat-centered-text
  - phosphor-duotone/check-circle
  - phosphor-duotone/arrow-right
  - phosphor-duotone/chart-line-up
  - phosphor-duotone/download-simple
  - phosphor-duotone/medal
  - phosphor-duotone/code
  - phosphor-duotone/lock-key
  - phosphor-duotone/compass

## images

- file: cover_bg.png
  dimensions: 1280x720
  acquire_via: ai
  layout_pattern: "#1 + #29"
  page_role: hero_page
  text_policy: none

- file: arch_isometric.png
  dimensions: 1280x720
  acquire_via: ai
  layout_pattern: "#44"
  page_role: local
  text_policy: none

- file: pipeline_schematic.png
  dimensions: 1280x720
  acquire_via: ai
  layout_pattern: "#44"
  page_role: local
  text_policy: none

## page_rhythm

P01: anchor
P02: dense
P03: dense
P04: dense
P05: breathing
P06: dense
P07: anchor
P08: dense
P09: dense
P10: dense
P11: dense
P12: dense
P13: dense
P14: anchor
P15: dense
P16: dense
P17: dense
P18: dense
P19: dense
P20: dense
P21: anchor

## page_charts

P02: timeline
P03: vertical_pillars
P04: vertical_list
P06: dumbbell_chart
P08: kpi_cards
P09: layered_architecture
P11: icon_grid
P12: pipeline_with_stages
P13: pipeline_with_stages
P14: hub_spoke
P19: numbered_steps
P20: basic_table
